package com.op1sync.app.feature.browser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.op1sync.app.core.audio.AudioPlayerManager
import com.op1sync.app.core.download.DownloadManager
import com.op1sync.app.core.download.DownloadStatus
import com.op1sync.app.core.usb.MtpConnectionManager
import com.op1sync.app.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class BrowserUiState(
    val currentPath: String = "/",
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isFolderDownloading: Boolean = false
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val mtpConnectionManager: MtpConnectionManager,
    val audioPlayerManager: AudioPlayerManager,
    val downloadManager: DownloadManager,
    private val libraryRepository: LibraryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BrowserUiState())
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()
    
    private val pathStack = mutableListOf<Int>()
    private var currentParentHandle: Int = -1 // -1 = root
    
    // Cache directory for audio preview
    private val cacheDir: File by lazy {
        File(context.cacheDir, "audio_preview").also { it.mkdirs() }
    }
    
    init {
        loadRootDirectory()
        observeFolderDownloadState()
    }
    
    private fun observeFolderDownloadState() {
        viewModelScope.launch {
            downloadManager.downloadState.collect { state ->
                _uiState.update { 
                    it.copy(isFolderDownloading = state.folderDownloadProgress != null)
                }
            }
        }
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
    
    fun playFile(item: FileItem) {
        if (item.isDirectory) return
        if (!isAudioFile(item.name)) return
        
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val mtp = mtpConnectionManager.getMtpDevice() ?: return@withContext
                    
                    // Cache file locally for playback
                    val cacheFile = File(cacheDir, item.name)
                    
                    // Only download if not already cached
                    if (!cacheFile.exists() || cacheFile.length() != item.size) {
                        mtp.importFile(item.handle, cacheFile.absolutePath)
                    }
                    
                    // Play the cached file
                    withContext(Dispatchers.Main) {
                        audioPlayerManager.playFromMtp(cacheFile.absolutePath, item.name)
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Nie udało się odtworzyć: ${e.message}") }
                }
            }
        }
    }
    
    fun downloadFile(item: FileItem) {
        if (downloadManager.isDownloading(item.handle)) return
        
        // Build relative path (remove leading /)
        val relativePath = _uiState.value.currentPath.removePrefix("/")
        val sourcePath = _uiState.value.currentPath + "/" + item.name
        
        viewModelScope.launch {
            val mtp = mtpConnectionManager.getMtpDevice()
            if (mtp == null) {
                _uiState.update { it.copy(error = "Urządzenie nie jest połączone") }
                return@launch
            }
            
            val result = downloadManager.downloadFile(
                mtpDevice = mtp,
                handle = item.handle,
                name = item.name,
                size = item.size,
                relativePath = relativePath
            )
            
            result.onSuccess { path ->
                // Save to library database
                viewModelScope.launch {
                    libraryRepository.addFile(
                        name = item.name,
                        path = path,
                        size = item.size,
                        sourcePath = sourcePath
                    )
                }
                _uiState.update { 
                    it.copy(successMessage = "Pobrano: ${item.name}")
                }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(error = "Błąd pobierania: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Download entire folder with all files recursively.
     */
    fun downloadFolder(item: FileItem) {
        if (!item.isDirectory) return
        if (downloadManager.isFolderDownloading()) {
            _uiState.update { it.copy(error = "Już trwa pobieranie folderu") }
            return
        }
        
        val basePath = _uiState.value.currentPath.removePrefix("/")
        
        viewModelScope.launch {
            val mtp = mtpConnectionManager.getMtpDevice()
            val storageId = mtpConnectionManager.getStorageId()
            
            if (mtp == null || storageId == null) {
                _uiState.update { it.copy(error = "Urządzenie nie jest połączone") }
                return@launch
            }
            
            _uiState.update { it.copy(successMessage = "Pobieranie folderu: ${item.name}...") }
            
            val result = downloadManager.downloadFolder(
                mtpDevice = mtp,
                storageId = storageId,
                folderHandle = item.handle,
                folderName = item.name,
                basePath = basePath,
                onFileCompleted = { name, path, sourcePath ->
                    // Save each file to library with correct source path
                    viewModelScope.launch {
                        libraryRepository.addFile(
                            name = name,
                            path = path,
                            size = File(path).length(),
                            sourcePath = sourcePath
                        )
                    }
                }
            )
            
            result.onSuccess { paths ->
                _uiState.update { 
                    it.copy(successMessage = "Pobrano ${paths.size} plików z ${item.name}")
                }
            }.onFailure { e ->
                _uiState.update { 
                    it.copy(error = "Błąd pobierania folderu: ${e.message}")
                }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
    
    fun togglePlayPause() {
        audioPlayerManager.togglePlayPause()
    }
    
    fun seekTo(percent: Float) {
        audioPlayerManager.seekToPercent(percent)
    }
    
    fun stopPlayback() {
        audioPlayerManager.stop()
    }
    
    private fun isAudioFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".wav") || lower.endsWith(".aif") || lower.endsWith(".aiff")
    }
    
    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.stop()
    }
}
