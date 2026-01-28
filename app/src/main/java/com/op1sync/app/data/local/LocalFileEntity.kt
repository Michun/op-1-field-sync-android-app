package com.op1sync.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_files")
data class LocalFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String,
    val size: Long,
    val type: FileType,
    val sourcePath: String, // Original path on OP-1 (e.g., "/tapes/2024-150#02/track1.aif")
    val folderName: String, // Parent folder name (e.g., "2024-150#02" for tapes)
    val downloadedAt: Long = System.currentTimeMillis(),
    val duration: Long? = null,
    val isFavorite: Boolean = false
)

enum class FileType {
    TAPE,
    SYNTH,
    DRUM,
    MIXDOWN,
    OTHER
}

/**
 * Represents a project folder in the library (e.g., a tape project).
 * Used for grouping files by their parent folder.
 */
data class ProjectFolder(
    val folderName: String,
    val type: FileType,
    val fileCount: Int,
    val totalSize: Long,
    val downloadedAt: Long,
    val isFavorite: Boolean,
    val files: List<LocalFileEntity> = emptyList()
)
