package com.op1sync.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocalFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localFileDao(): LocalFileDao
}
