package com.op1sync.app.feature.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.op1sync.app.core.usb.MtpConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BrowserUiState(
    val currentPath: String = "/",
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val mtpConnectionManager: MtpConnectionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()
    
    private val pathStack = mutableListOf<Int>()
    private var currentParentHandle: Int = -1 // -1 = root
    
    init {
        loadRootDirectory()
    }
    
    private fun loadRootDirectory() {
        viewModelScope.launch {
            loadDirectory(-1, "/")
        }
    }
    
    private suspend fun loadDirectory(parentHandle: Int, path: String) {
        _uiState.update { it.copy(isLoading = true) }
        
        withContext(Dispatchers.IO) {
            try {
                val mtp = mtpConnectionManager.getMtpDevice()
                val storageId = mtpConnectionManager.getStorageId()
                
                if (mtp == null || storageId == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Urządzenie nie jest połączone"
                        )
                    }
                    return@withContext
                }
                
                val handles = mtp.getObjectHandles(storageId, 0, parentHandle)
                val items = mutableListOf<FileItem>()
                
                handles?.forEach { handle ->
                    val info = mtp.getObjectInfo(handle)
                    if (info != null) {
                        items.add(
                            FileItem(
                                name = info.name ?: "Unknown",
                                isDirectory = info.format == 0x3001,
                                size = info.compressedSize.toLong(),
                                handle = handle
                            )
                        )
                    }
                }
                
                // Sort: directories first, then by name
                val sortedItems = items.sortedWith(
                    compareByDescending<FileItem> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                )
                
                currentParentHandle = parentHandle
                
                _uiState.update {
                    it.copy(
                        currentPath = path,
                        items = sortedItems,
                        isLoading = false,
                        canGoBack = pathStack.isNotEmpty(),
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    fun navigateToFolder(item: FileItem) {
        if (!item.isDirectory) return
        
        pathStack.add(currentParentHandle)
        val newPath = if (_uiState.value.currentPath == "/") {
            "/${item.name}"
        } else {
            "${_uiState.value.currentPath}/${item.name}"
        }
        
        viewModelScope.launch {
            loadDirectory(item.handle, newPath)
        }
    }
    
    fun navigateUp() {
        if (pathStack.isEmpty()) return
        
        val previousHandle = pathStack.removeAt(pathStack.size - 1)
        val currentParts = _uiState.value.currentPath.split("/").filter { it.isNotEmpty() }
        val newPath = if (currentParts.size <= 1) {
            "/"
        } else {
            "/" + currentParts.dropLast(1).joinToString("/")
        }
        
        viewModelScope.launch {
            loadDirectory(previousHandle, newPath)
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            loadDirectory(currentParentHandle, _uiState.value.currentPath)
        }
    }
    
    fun selectFile(item: FileItem) {
        // TODO: Preview/play audio file
    }
    
    fun downloadFile(item: FileItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val mtp = mtpConnectionManager.getMtpDevice() ?: return@withContext
                    
                    // Get app's external files directory
                    val destDir = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_MUSIC
                    )
                    val destFile = java.io.File(destDir, "OP1Sync/${item.name}")
                    destFile.parentFile?.mkdirs()
                    
                    mtp.importFile(item.handle, destFile.absolutePath)
                    
                    // TODO: Notify success
                } catch (e: Exception) {
                    // TODO: Handle error
                }
            }
        }
    }
}
