package com.op1sync.app.di

import android.content.Context
import androidx.room.Room
import com.op1sync.app.core.audio.AudioPlayerManager
import com.op1sync.app.core.download.DownloadManager
import com.op1sync.app.core.usb.MtpConnectionManager
import com.op1sync.app.data.local.AppDatabase
import com.op1sync.app.data.local.LocalFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "op1sync_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideLocalFileDao(database: AppDatabase): LocalFileDao {
        return database.localFileDao()
    }
    
    @Provides
    @Singleton
    fun provideMtpConnectionManager(
        @ApplicationContext context: Context
    ): MtpConnectionManager {
        return MtpConnectionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAudioPlayerManager(
        @ApplicationContext context: Context
    ): AudioPlayerManager {
        return AudioPlayerManager(context)
    }
    
    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context
    ): DownloadManager {
        return DownloadManager(context)
    }
}
