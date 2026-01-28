package com.op1sync.app.core.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentTrack: TrackInfo? = null,
    val position: Long = 0L,
    val duration: Long = 0L,
    val isBuffering: Boolean = false,
    val error: String? = null
)

data class TrackInfo(
    val name: String,
    val path: String,
    val uri: Uri
)

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioPlayerManager"
    }
    
    private var exoPlayer: ExoPlayer? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            Log.d(TAG, "Playback state: $state")
            updatePlaybackState()
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePlaybackState()
        }
        
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error: ${error.errorCode} - ${error.message}", error)
            _playbackState.value = _playbackState.value.copy(
                error = "Błąd odtwarzania: ${error.message}",
                isPlaying = false
            )
        }
    }
    
    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: run {
            // Create ExtractorsFactory with AIFF support
            val extractorsFactory = DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)
            
            // Create MediaSourceFactory with custom extractors
            val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
            
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .also {
                    it.addListener(playerListener)
                    exoPlayer = it
                }
        }
    }
    
    fun play(uri: Uri, name: String) {
        val player = getOrCreatePlayer()
        
        val trackInfo = TrackInfo(
            name = name,
            path = uri.path ?: "",
            uri = uri
        )
        
        _playbackState.value = _playbackState.value.copy(
            currentTrack = trackInfo,
            isBuffering = true
        )
        
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
    
    fun playFromMtp(localCachePath: String, name: String) {
        // Use Uri.fromFile to properly handle paths with special characters like #
        val uri = Uri.fromFile(java.io.File(localCachePath))
        play(uri, name)
    }
    
    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }
    
    fun pause() {
        exoPlayer?.pause()
    }
    
    fun resume() {
        exoPlayer?.play()
    }
    
    fun stop() {
        exoPlayer?.stop()
        _playbackState.value = PlaybackState()
    }
    
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }
    
    fun seekToPercent(percent: Float) {
        exoPlayer?.let { player ->
            val position = (player.duration * percent).toLong()
            player.seekTo(position)
        }
    }
    
    fun updatePosition() {
        updatePlaybackState()
    }
    
    private fun updatePlaybackState() {
        exoPlayer?.let { player ->
            _playbackState.value = _playbackState.value.copy(
                isPlaying = player.isPlaying,
                position = player.currentPosition.coerceAtLeast(0),
                duration = player.duration.coerceAtLeast(0),
                isBuffering = player.playbackState == Player.STATE_BUFFERING
            )
        }
    }
    
    fun release() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        _playbackState.value = PlaybackState()
    }
}
