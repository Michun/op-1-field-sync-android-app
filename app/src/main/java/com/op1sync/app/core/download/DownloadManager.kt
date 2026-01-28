package com.op1sync.app.core.download

import android.content.Context
import android.mtp.MtpDevice
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadItem(
    val handle: Int,
    val name: String,
    val size: Long,
    val relativePath: String = "", // Path relative to OP-1 root (e.g., "tapes/2024-150#02")
    val status: DownloadStatus = DownloadStatus.Pending,
    val progress: Float = 0f,
    val localPath: String? = null,
    val error: String? = null
)

sealed class DownloadStatus {
    data object Pending : DownloadStatus()
    data object InProgress : DownloadStatus()
    data object Completed : DownloadStatus()
    data object Failed : DownloadStatus()
}

data class DownloadState(
    val activeDownloads: Map<Int, DownloadItem> = emptyMap(),
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val folderDownloadProgress: FolderDownloadProgress? = null
)

data class FolderDownloadProgress(
    val folderName: String,
    val totalFiles: Int,
    val completedFiles: Int,
    val currentFile: String
)

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DownloadManager"
        private const val DOWNLOAD_FOLDER = "OP1Sync"
    }
    
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    private val downloadDir: File by lazy {
        // Use app-specific external storage - no permissions needed on Android 10+
        // Files are stored in /Android/data/com.op1sync.app/files/OP1Sync/
        val appDir = context.getExternalFilesDir(null)
        File(appDir, DOWNLOAD_FOLDER).also { 
            val created = it.mkdirs()
            Log.d(TAG, "Download dir: ${it.absolutePath}, exists: ${it.exists()}, created: $created")
        }
    }
    
    /**
     * Download a single file preserving the OP-1 folder structure.
     * @param relativePath The path relative to OP-1 root (e.g., "tapes/2024-150#02")
     */
    suspend fun downloadFile(
        mtpDevice: MtpDevice,
        handle: Int,
        name: String,
        size: Long,
        relativePath: String = "",
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        
        Log.d(TAG, "Starting download: $relativePath/$name (${formatSize(size)})")
        
        val downloadItem = DownloadItem(
            handle = handle,
            name = name,
            size = size,
            relativePath = relativePath,
            status = DownloadStatus.InProgress
        )
        _downloadState.update { state ->
            state.copy(activeDownloads = state.activeDownloads + (handle to downloadItem))
        }
        
        try {
            // Create destination preserving folder structure
            val destDir = if (relativePath.isNotEmpty()) {
                File(downloadDir, relativePath).also { it.mkdirs() }
            } else {
                downloadDir
            }
            val destFile = File(destDir, name)
            
            updateProgress(handle, 0.1f)
            onProgress?.invoke(0.1f)
            
            val success = mtpDevice.importFile(handle, destFile.absolutePath)
            
            if (success) {
                updateProgress(handle, 1.0f)
                onProgress?.invoke(1.0f)
                
                _downloadState.update { state ->
                    val updated = state.activeDownloads[handle]?.copy(
                        status = DownloadStatus.Completed,
                        progress = 1.0f,
                        localPath = destFile.absolutePath
                    )
                    state.copy(
                        activeDownloads = if (updated != null) 
                            state.activeDownloads + (handle to updated) 
                        else state.activeDownloads,
                        completedCount = state.completedCount + 1
                    )
                }
                
                Log.d(TAG, "Download completed: $name -> ${destFile.absolutePath}")
                Result.success(destFile.absolutePath)
            } else {
                throw Exception("MTP import failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: $name", e)
            
            _downloadState.update { state ->
                val updated = state.activeDownloads[handle]?.copy(
                    status = DownloadStatus.Failed,
                    error = e.message
                )
                state.copy(
                    activeDownloads = if (updated != null)
                        state.activeDownloads + (handle to updated)
                    else state.activeDownloads,
                    failedCount = state.failedCount + 1
                )
            }
            
            Result.failure(e)
        }
    }
    
    /**
     * Download all files in a folder recursively, preserving structure.
     * Returns list of successfully downloaded file paths.
     */
    suspend fun downloadFolder(
        mtpDevice: MtpDevice,
        storageId: Int,
        folderHandle: Int,
        folderName: String,
        basePath: String = "",
        onFileCompleted: ((name: String, localPath: String, sourcePath: String) -> Unit)? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        
        val downloadedPaths = mutableListOf<String>()
        val folderPath = if (basePath.isEmpty()) folderName else "$basePath/$folderName"
        
        Log.d(TAG, "Starting folder download: $folderPath")
        
        try {
            // Get all items in folder
            val items = collectFolderItems(mtpDevice, storageId, folderHandle, folderPath)
            val files = items.filter { !it.isDirectory }
            
            Log.d(TAG, "Found ${files.size} files in $folderPath")
            
            // Update folder progress
            _downloadState.update { it.copy(
                folderDownloadProgress = FolderDownloadProgress(
                    folderName = folderName,
                    totalFiles = files.size,
                    completedFiles = 0,
                    currentFile = ""
                )
            )}
            
            var completed = 0
            
            for (item in files) {
                // Update current file in progress
                _downloadState.update { state ->
                    state.copy(
                        folderDownloadProgress = state.folderDownloadProgress?.copy(
                            currentFile = item.name,
                            completedFiles = completed
                        )
                    )
                }
                
                val result = downloadFile(
                    mtpDevice = mtpDevice,
                    handle = item.handle,
                    name = item.name,
                    size = item.size,
                    relativePath = item.relativePath
                )
                
                result.onSuccess { path ->
                    downloadedPaths.add(path)
                    // Pass the full source path including subcategory
                    val sourcePath = "/${item.relativePath}/${item.name}"
                    onFileCompleted?.invoke(item.name, path, sourcePath)
                }
                
                completed++
            }
            
            // Clear folder progress
            _downloadState.update { it.copy(folderDownloadProgress = null) }
            
            Log.d(TAG, "Folder download completed: ${downloadedPaths.size}/${files.size} files")
            
            Result.success(downloadedPaths)
            
        } catch (e: Exception) {
            Log.e(TAG, "Folder download failed: $folderPath", e)
            _downloadState.update { it.copy(folderDownloadProgress = null) }
            Result.failure(e)
        }
    }
    
    /**
     * Recursively collect all files in a folder.
     */
    private fun collectFolderItems(
        mtpDevice: MtpDevice,
        storageId: Int,
        parentHandle: Int,
        currentPath: String
    ): List<FolderItem> {
        val items = mutableListOf<FolderItem>()
        
        Log.d(TAG, "collectFolderItems: storageId=$storageId, parentHandle=$parentHandle, path=$currentPath")
        
        val handles = mtpDevice.getObjectHandles(storageId, 0, parentHandle)
        
        if (handles == null) {
            Log.w(TAG, "getObjectHandles returned null for parentHandle=$parentHandle")
            return items
        }
        
        Log.d(TAG, "Found ${handles.size} items in $currentPath")
        
        for (handle in handles) {
            val info = mtpDevice.getObjectInfo(handle)
            if (info == null) {
                Log.w(TAG, "getObjectInfo returned null for handle=$handle")
                continue
            }
            
            val name = info.name ?: continue
            // MTP format codes: 0x3001 = folder, 0x3000 = undefined, others = specific formats
            val isDirectory = info.format == 0x3001 || info.format == 0x3000 && info.compressedSize == 0
            
            Log.d(TAG, "  Item: $name (format=${info.format}, size=${info.compressedSize}, isDir=$isDirectory)")
            
            if (isDirectory) {
                // Recurse into subdirectory
                val subPath = "$currentPath/$name"
                items.addAll(collectFolderItems(mtpDevice, storageId, handle, subPath))
            } else {
                items.add(FolderItem(
                    handle = handle,
                    name = name,
                    size = info.compressedSize.toLong(),
                    relativePath = currentPath,
                    isDirectory = false
                ))
            }
        }
        
        return items
    }
    
    private data class FolderItem(
        val handle: Int,
        val name: String,
        val size: Long,
        val relativePath: String,
        val isDirectory: Boolean
    )
    
    private fun updateProgress(handle: Int, progress: Float) {
        _downloadState.update { state ->
            val updated = state.activeDownloads[handle]?.copy(progress = progress)
            if (updated != null) {
                state.copy(activeDownloads = state.activeDownloads + (handle to updated))
            } else state
        }
    }
    
    fun clearCompleted() {
        _downloadState.update { state ->
            val filtered = state.activeDownloads.filterValues { 
                it.status != DownloadStatus.Completed && it.status != DownloadStatus.Failed
            }
            state.copy(activeDownloads = filtered)
        }
    }
    
    fun getDownloadStatus(handle: Int): DownloadItem? {
        return _downloadState.value.activeDownloads[handle]
    }
    
    fun isDownloading(handle: Int): Boolean {
        return _downloadState.value.activeDownloads[handle]?.status == DownloadStatus.InProgress
    }
    
    fun isFolderDownloading(): Boolean {
        return _downloadState.value.folderDownloadProgress != null
    }
    
    fun getLocalPath(handle: Int): String? {
        return _downloadState.value.activeDownloads[handle]?.localPath
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
