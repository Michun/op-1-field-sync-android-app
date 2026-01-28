package com.op1sync.app.data.repository

import com.op1sync.app.data.local.FileType
import com.op1sync.app.data.local.LocalFileDao
import com.op1sync.app.data.local.LocalFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    
    fun searchFiles(query: String): Flow<List<LocalFileEntity>> = 
        localFileDao.searchFiles(query)
    
    fun getTotalCount(): Flow<Int> = localFileDao.getTotalCount()
    
    suspend fun addFile(
        name: String,
        path: String,
        size: Long,
        sourcePath: String,
        duration: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val type = determineFileType(sourcePath)
        val entity = LocalFileEntity(
            name = name,
            path = path,
            size = size,
            type = type,
            sourcePath = sourcePath,
            duration = duration
        )
        localFileDao.insertFile(entity)
    }
    
    suspend fun deleteFile(file: LocalFileEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete actual file
            val physicalFile = File(file.path)
            if (physicalFile.exists()) {
                physicalFile.delete()
            }
            // Delete from database
            localFileDao.deleteFile(file)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun toggleFavorite(file: LocalFileEntity) = withContext(Dispatchers.IO) {
        localFileDao.setFavorite(file.id, !file.isFavorite)
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
}
