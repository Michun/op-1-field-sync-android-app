package com.op1sync.app.core.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.mtp.MtpDevice
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ConnectionState(
    val isConnected: Boolean = false,
    val deviceName: String? = null,
    val storageId: Int? = null,
    val errorMessage: String? = null
)

data class DeviceStats(
    val tapesCount: Int = 0,
    val synthCount: Int = 0,
    val drumCount: Int = 0,
    val mixdownCount: Int = 0
)

@Singleton
class MtpConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MtpConnectionManager"
        private const val ACTION_USB_PERMISSION = "com.op1sync.USB_PERMISSION"
        private const val OP1_FIELD_VENDOR_ID = 9063 // 0x2367 - Teenage Engineering
    }
    
    private val usbManager: UsbManager = 
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    
    private var mtpDevice: MtpDevice? = null
    private var currentStorageId: Int? = null
    private var isReceiverRegistered = false
    
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Received broadcast: ${intent.action}")
            
            try {
                when (intent.action) {
                    ACTION_USB_PERMISSION -> {
                        synchronized(this) {
                            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                            }
                            
                            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                            Log.d(TAG, "USB permission granted: $granted for device: ${device?.productName}")
                            
                            if (granted && device != null) {
                                openMtpDevice(device)
                            } else {
                                _connectionState.value = ConnectionState(
                                    isConnected = false,
                                    errorMessage = "Brak uprawnień USB"
                                )
                            }
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        Log.d(TAG, "USB device attached")
                        // Device attached - try to find OP-1 and request permission
                        findOP1Field()?.let { device ->
                            Log.d(TAG, "Found OP-1 Field: ${device.productName}")
                            if (usbManager.hasPermission(device)) {
                                openMtpDevice(device)
                            } else {
                                requestPermission(device)
                            }
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        Log.d(TAG, "USB device detached")
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        
                        // Only disconnect if it's our device
                        if (device?.vendorId == OP1_FIELD_VENDOR_ID) {
                            disconnect()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling USB broadcast", e)
                _connectionState.value = ConnectionState(
                    isConnected = false,
                    errorMessage = "Błąd: ${e.message}"
                )
            }
        }
    }
    
    init {
        registerReceiver()
        // Check for already connected device on init
        checkForConnectedDevice()
    }
    
    private fun checkForConnectedDevice() {
        try {
            findOP1Field()?.let { device ->
                Log.d(TAG, "Found already connected OP-1: ${device.productName}")
                if (usbManager.hasPermission(device)) {
                    openMtpDevice(device)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for connected device", e)
        }
    }
    
    private fun registerReceiver() {
        if (isReceiverRegistered) return
        
        try {
            val filter = IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For USB system broadcasts, we need RECEIVER_EXPORTED
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(usbReceiver, filter)
            }
            isReceiverRegistered = true
            Log.d(TAG, "USB receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering receiver", e)
        }
    }
    
    private fun findOP1Field(): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            device.vendorId == OP1_FIELD_VENDOR_ID
        }
    }
    
    private fun requestPermission(device: UsbDevice) {
        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION).apply {
                    setPackage(context.packageName)
                },
                flags
            )
            usbManager.requestPermission(device, permissionIntent)
            Log.d(TAG, "Permission requested for device: ${device.productName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting permission", e)
        }
    }
    
    private fun openMtpDevice(usbDevice: UsbDevice) {
        try {
            Log.d(TAG, "Opening MTP device: ${usbDevice.productName}")
            
            // Close existing connection if any
            mtpDevice?.close()
            mtpDevice = null
            currentStorageId = null
            
            val connection = usbManager.openDevice(usbDevice)
            if (connection == null) {
                Log.e(TAG, "Failed to open USB device connection")
                _connectionState.value = ConnectionState(
                    isConnected = false,
                    errorMessage = "Nie można otworzyć połączenia USB"
                )
                return
            }
            
            val mtp = MtpDevice(usbDevice)
            if (mtp.open(connection)) {
                mtpDevice = mtp
                Log.d(TAG, "MTP device opened successfully")
                
                // Get storage
                val storageIds = mtp.storageIds
                if (storageIds != null && storageIds.isNotEmpty()) {
                    currentStorageId = storageIds[0]
                    Log.d(TAG, "Storage ID: ${currentStorageId}")
                } else {
                    Log.w(TAG, "No storage found on device")
                }
                
                val deviceInfo = mtp.deviceInfo
                _connectionState.value = ConnectionState(
                    isConnected = true,
                    deviceName = deviceInfo?.model ?: usbDevice.productName ?: "OP-1 Field",
                    storageId = currentStorageId,
                    errorMessage = null
                )
            } else {
                Log.e(TAG, "Failed to open MTP protocol")
                connection.close()
                _connectionState.value = ConnectionState(
                    isConnected = false,
                    errorMessage = "Nie można otworzyć MTP. Sprawdź czy OP-1 jest w trybie MTP."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MTP device", e)
            _connectionState.value = ConnectionState(
                isConnected = false,
                errorMessage = "Błąd: ${e.message}"
            )
        }
    }
    
    fun connect() {
        findOP1Field()?.let { device ->
            if (usbManager.hasPermission(device)) {
                openMtpDevice(device)
            } else {
                requestPermission(device)
            }
        } ?: run {
            _connectionState.value = ConnectionState(
                isConnected = false,
                errorMessage = "Nie znaleziono OP-1 Field. Podłącz urządzenie."
            )
        }
    }
    
    fun disconnect() {
        try {
            mtpDevice?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing MTP device", e)
        }
        mtpDevice = null
        currentStorageId = null
        _connectionState.value = ConnectionState(isConnected = false)
        Log.d(TAG, "Disconnected")
    }
    
    suspend fun getDeviceStats(): DeviceStats = withContext(Dispatchers.IO) {
        val mtp = mtpDevice ?: return@withContext DeviceStats()
        val storageId = currentStorageId ?: return@withContext DeviceStats()
        
        var tapesCount = 0
        var synthCount = 0
        var drumCount = 0
        var mixdownCount = 0
        
        try {
            // Get root object handles
            val rootHandles = mtp.getObjectHandles(storageId, 0, -1)
            
            rootHandles?.forEach { handle ->
                val info = mtp.getObjectInfo(handle)
                when (info?.name?.lowercase()) {
                    "tapes" -> {
                        val tapeHandles = mtp.getObjectHandles(storageId, 0, handle)
                        tapesCount = tapeHandles?.filter { h ->
                            val tapeInfo = mtp.getObjectInfo(h)
                            tapeInfo?.format == 0x3001 // Directory format
                        }?.size ?: 0
                    }
                    "synth" -> {
                        synthCount = countFilesRecursive(mtp, storageId, handle)
                    }
                    "drum" -> {
                        drumCount = countFilesRecursive(mtp, storageId, handle)
                    }
                    "mixdown" -> {
                        val mixHandles = mtp.getObjectHandles(storageId, 0, handle)
                        mixdownCount = mixHandles?.filter { h ->
                            val mixInfo = mtp.getObjectInfo(h)
                            mixInfo?.name?.endsWith(".wav") == true
                        }?.size ?: 0
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device stats", e)
        }
        
        DeviceStats(tapesCount, synthCount, drumCount, mixdownCount)
    }
    
    private fun countFilesRecursive(mtp: MtpDevice, storageId: Int, parentHandle: Int): Int {
        var count = 0
        val handles = mtp.getObjectHandles(storageId, 0, parentHandle) ?: return 0
        
        handles.forEach { handle ->
            val info = mtp.getObjectInfo(handle)
            if (info != null) {
                if (info.format == 0x3001) { // Directory
                    count += countFilesRecursive(mtp, storageId, handle)
                } else if (info.name?.endsWith(".wav") == true || info.name?.endsWith(".aif") == true) {
                    count++
                }
            }
        }
        return count
    }
    
    fun getMtpDevice(): MtpDevice? = mtpDevice
    fun getStorageId(): Int? = currentStorageId
}
