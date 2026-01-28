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
    val storageId: Int? = null
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
        private const val ACTION_USB_PERMISSION = "com.op1sync.USB_PERMISSION"
        private const val OP1_FIELD_VENDOR_ID = 9063 // 0x2367 - Teenage Engineering
    }
    
    private val usbManager: UsbManager = 
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    
    private var mtpDevice: MtpDevice? = null
    private var currentStorageId: Int? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { openMtpDevice(it) }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    findOP1Field()?.let { requestPermission(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    disconnect()
                }
            }
        }
    }
    
    init {
        registerReceiver()
        // Check for already connected device
        findOP1Field()?.let { device ->
            if (usbManager.hasPermission(device)) {
                openMtpDevice(device)
            }
        }
    }
    
    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }
    
    private fun findOP1Field(): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            device.vendorId == OP1_FIELD_VENDOR_ID
        }
    }
    
    private fun requestPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }
    
    private fun openMtpDevice(usbDevice: UsbDevice) {
        try {
            val connection = usbManager.openDevice(usbDevice)
            if (connection != null) {
                val mtp = MtpDevice(usbDevice)
                if (mtp.open(connection)) {
                    mtpDevice = mtp
                    
                    // Get storage
                    val storageIds = mtp.storageIds
                    if (storageIds != null && storageIds.isNotEmpty()) {
                        currentStorageId = storageIds[0]
                    }
                    
                    val deviceInfo = mtp.deviceInfo
                    _connectionState.value = ConnectionState(
                        isConnected = true,
                        deviceName = deviceInfo?.model ?: "OP-1 Field",
                        storageId = currentStorageId
                    )
                }
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState(isConnected = false)
        }
    }
    
    fun connect() {
        findOP1Field()?.let { device ->
            if (usbManager.hasPermission(device)) {
                openMtpDevice(device)
            } else {
                requestPermission(device)
            }
        }
    }
    
    fun disconnect() {
        mtpDevice?.close()
        mtpDevice = null
        currentStorageId = null
        _connectionState.value = ConnectionState(isConnected = false)
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
            // Log error
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
