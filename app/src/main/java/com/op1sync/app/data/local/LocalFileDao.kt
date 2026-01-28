package com.op1sync.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalFileDao {
    
    @Query("SELECT * FROM local_files ORDER BY downloadedAt DESC")
    fun getAllFiles(): Flow<List<LocalFileEntity>>
    
    @Query("SELECT * FROM local_files WHERE type = :type ORDER BY downloadedAt DESC")
    fun getFilesByType(type: FileType): Flow<List<LocalFileEntity>>
    
    @Query("SELECT * FROM local_files WHERE isFavorite = 1 ORDER BY downloadedAt DESC")
    fun getFavorites(): Flow<List<LocalFileEntity>>
    
    @Query("SELECT * FROM local_files WHERE name LIKE '%' || :query || '%' ORDER BY downloadedAt DESC")
    fun searchFiles(query: String): Flow<List<LocalFileEntity>>
    
    @Query("SELECT * FROM local_files WHERE id = :id")
    suspend fun getFileById(id: Long): LocalFileEntity?
    
    @Query("SELECT * FROM local_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): LocalFileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: LocalFileEntity): Long
    
    @Update
    suspend fun updateFile(file: LocalFileEntity)
    
    @Delete
    suspend fun deleteFile(file: LocalFileEntity)
    
    @Query("DELETE FROM local_files WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("UPDATE local_files SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    
    @Query("SELECT COUNT(*) FROM local_files")
    fun getTotalCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM local_files WHERE type = :type")
    fun getCountByType(type: FileType): Flow<Int>
}
