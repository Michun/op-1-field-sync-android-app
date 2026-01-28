package com.op1sync.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LocalFileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localFileDao(): LocalFileDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add folderName column with default value
                db.execSQL("ALTER TABLE local_files ADD COLUMN folderName TEXT NOT NULL DEFAULT 'unknown'")
            }
        }
    }
}
