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
    val sourcePath: String, // Original path on OP-1
    val downloadedAt: Long = System.currentTimeMillis(),
    val duration: Long? = null, // For audio files, in milliseconds
    val isFavorite: Boolean = false
)

enum class FileType {
    TAPE,
    SYNTH,
    DRUM,
    MIXDOWN,
    OTHER
}
