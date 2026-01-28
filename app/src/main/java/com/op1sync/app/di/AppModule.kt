package com.op1sync.app.di

import android.content.Context
import com.op1sync.app.core.audio.AudioPlayerManager
import com.op1sync.app.core.usb.MtpConnectionManager
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
}

