package com.op1sync.app.data.repository

import com.op1sync.app.data.local.FileType
import com.op1sync.app.data.local.LocalFileDao
import com.op1sync.app.data.local.LocalFileEntity
import com.op1sync.app.data.local.ProjectFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val localFileDao: LocalFileDao
) {
    
    fun getAllFiles(): Flow<List<LocalFileEntity>> = localFileDao.getAllFiles()
    
    fun getFilesByType(type: FileType): Flow<List<LocalFileEntity>> = 
        localFileDao.getFilesByType(type)
    
    fun getFavorites(): Flow<List<LocalFileEntity>> = localFileDao.getFavorites()
    
    fun getFilesByFolder(folderName: String): Flow<List<LocalFileEntity>> =
        localFileDao.getFilesByFolder(folderName)
    
    fun searchFiles(query: String): Flow<List<LocalFileEntity>> = 
        localFileDao.searchFiles(query)
    
    fun getTotalCount(): Flow<Int> = localFileDao.getTotalCount()
    
    /**
     * Get all files grouped by folder as ProjectFolder objects.
     */
    fun getProjectFolders(): Flow<List<ProjectFolder>> = 
        localFileDao.getAllFiles().map { files ->
            groupFilesIntoFolders(files)
        }
    
    /**
     * Get project folders filtered by type.
     */
    fun getProjectFoldersByType(type: FileType): Flow<List<ProjectFolder>> =
        localFileDao.getFilesByType(type).map { files ->
            groupFilesIntoFolders(files)
        }
    
    /**
     * Get favorite project folders.
     */
    fun getFavoriteProjectFolders(): Flow<List<ProjectFolder>> =
        localFileDao.getFavorites().map { files ->
            groupFilesIntoFolders(files)
        }
    
    private fun groupFilesIntoFolders(files: List<LocalFileEntity>): List<ProjectFolder> {
        return files
            .groupBy { it.folderName }
            .map { (folderName, folderFiles) ->
                ProjectFolder(
                    folderName = folderName,
                    type = folderFiles.first().type,
                    fileCount = folderFiles.size,
                    totalSize = folderFiles.sumOf { it.size },
                    downloadedAt = folderFiles.maxOf { it.downloadedAt },
                    isFavorite = folderFiles.any { it.isFavorite },
                    files = folderFiles.sortedBy { it.name }
                )
            }
            .sortedByDescending { it.downloadedAt }
    }
    
    suspend fun addFile(
        name: String,
        path: String,
        size: Long,
        sourcePath: String,
        duration: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val type = determineFileType(sourcePath)
        val folderName = extractFolderName(sourcePath, type)
        val entity = LocalFileEntity(
            name = name,
            path = path,
            size = size,
            type = type,
            sourcePath = sourcePath,
            folderName = folderName,
            duration = duration
        )
        localFileDao.insertFile(entity)
    }
    
    suspend fun deleteFile(file: LocalFileEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val physicalFile = File(file.path)
            if (physicalFile.exists()) {
                physicalFile.delete()
            }
            localFileDao.deleteFile(file)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun deleteFolder(folder: ProjectFolder): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete all physical files in the folder
            folder.files.forEach { file ->
                val physicalFile = File(file.path)
                if (physicalFile.exists()) {
                    physicalFile.delete()
                }
            }
            // Try to delete the folder itself if empty
            folder.files.firstOrNull()?.path?.let { path ->
                File(path).parentFile?.let { dir ->
                    if (dir.exists() && dir.listFiles()?.isEmpty() == true) {
                        dir.delete()
                    }
                }
            }
            // Delete from database
            localFileDao.deleteByFolder(folder.folderName)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun toggleFavorite(file: LocalFileEntity) = withContext(Dispatchers.IO) {
        localFileDao.setFavorite(file.id, !file.isFavorite)
    }
    
    suspend fun toggleFolderFavorite(folder: ProjectFolder) = withContext(Dispatchers.IO) {
        localFileDao.setFolderFavorite(folder.folderName, !folder.isFavorite)
    }
    
    suspend fun getFileByPath(path: String): LocalFileEntity? = 
        localFileDao.getFileByPath(path)
    
    suspend fun fileExists(path: String): Boolean = 
        localFileDao.getFileByPath(path) != null
    
    private fun determineFileType(sourcePath: String): FileType {
        val lower = sourcePath.lowercase()
        return when {
            lower.contains("/tapes/") -> FileType.TAPE
            lower.contains("/synth/") -> FileType.SYNTH
            lower.contains("/drum/") -> FileType.DRUM
            lower.contains("/mixdown/") -> FileType.MIXDOWN
            else -> FileType.OTHER
        }
    }
    
    /**
     * Extract the folder name from source path.
     * For tapes: /tapes/2024-150#02/track1.aif -> "2024-150#02"
     * For synth/drum: /synth/bass/preset.aif -> "bass"
     * For mixdown: /mixdown/song.wav -> "mixdown"
     */
    private fun extractFolderName(sourcePath: String, type: FileType): String {
        val parts = sourcePath.split("/").filter { it.isNotEmpty() }
        
        return when (type) {
            FileType.TAPE -> {
                // /tapes/2024-150#02/track1.aif -> "2024-150#02"
                val tapesIndex = parts.indexOfFirst { it.equals("tapes", ignoreCase = true) }
                if (tapesIndex >= 0 && tapesIndex + 1 < parts.size - 1) {
                    parts[tapesIndex + 1]
                } else {
                    parts.getOrElse(1) { "Unknown" }
                }
            }
            FileType.SYNTH, FileType.DRUM -> {
                // /synth/bass/preset.aif -> "bass"
                val typeIndex = parts.indexOfFirst { 
                    it.equals("synth", ignoreCase = true) || it.equals("drum", ignoreCase = true) 
                }
                if (typeIndex >= 0 && typeIndex + 1 < parts.size - 1) {
                    parts[typeIndex + 1]
                } else {
                    type.name.lowercase()
                }
            }
            FileType.MIXDOWN -> "mixdown"
            FileType.OTHER -> parts.getOrElse(parts.size - 2) { "other" }
        }
    }
}
