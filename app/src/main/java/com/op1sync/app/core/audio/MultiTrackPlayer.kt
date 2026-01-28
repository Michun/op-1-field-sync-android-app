package com.op1sync.app.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-track audio player that mixes up to 4 WAV tracks simultaneously.
 * Handles OP-1 Field tape format: 32-bit signed integer stereo WAV files.
 * Uses 32-bit float output for high quality mixing.
 */
@Singleton
class MultiTrackPlayer @Inject constructor() {
    
    companion object {
        private const val TAG = "MultiTrackPlayer"
        private const val SAMPLE_RATE = 44100
        
        // Input format: 32-bit signed integer stereo
        private const val INPUT_BYTES_PER_SAMPLE = 4  // 32-bit integer
        private const val INPUT_CHANNELS = 2  // Stereo
        private const val INPUT_BYTES_PER_FRAME = INPUT_BYTES_PER_SAMPLE * INPUT_CHANNELS  // 8 bytes
        
        // Output format: 32-bit float stereo
        private const val OUTPUT_FLOATS_PER_FRAME = 2  // Stereo
        
        private const val FRAMES_PER_BUFFER = 2048
        
        // Max value for 32-bit signed integer
        private const val INT32_MAX = 2147483647.0f
    }
    
    private var trackFiles: List<RandomAccessFile?> = emptyList()
    private var trackDataOffsets: List<Long> = emptyList()
    private var trackMuted: Array<Boolean> = arrayOf(false, false, false, false)
    
    private val minBufferSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_FLOAT
    ).coerceAtLeast(FRAMES_PER_BUFFER * OUTPUT_FLOATS_PER_FRAME * 4)
    
    private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null
    
    // Position in frames (44100 frames = 1 second)
    private val currentFramePosition = AtomicLong(0L)
    private val playerState = AtomicReference(PlayerState.STOPPED)
    private val playLock = Object()
    
    private val _playbackState = MutableStateFlow(MultiTrackPlaybackState())
    val playbackState: StateFlow<MultiTrackPlaybackState> = _playbackState.asStateFlow()
    
    val isPlaying: Boolean
        get() = playerState.get() == PlayerState.PLAYING
    
    val currentPositionSamples: Long
        get() = currentFramePosition.get()
    
    val currentPositionMs: Long
        get() = (currentFramePosition.get() * 1000) / SAMPLE_RATE
    
    /**
     * Load track files for playback.
     */
    fun prepare(trackFilePaths: List<String>) {
        release()
        
        trackFiles = trackFilePaths.map { path ->
            if (path.isNotEmpty() && File(path).exists()) {
                try {
                    RandomAccessFile(path, "r")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open track: $path", e)
                    null
                }
            } else {
                null
            }
        }
        
        // Find data chunk offset for each WAV file
        trackDataOffsets = trackFiles.map { file ->
            if (file != null) {
                findWavDataOffset(file)
            } else {
                0L
            }
        }
        
        // Enable tracks that have files (not muted by default)
        trackMuted = trackFiles.map { it == null }.toTypedArray()
        
        Log.d(TAG, "Prepared ${trackFiles.count { it != null }} tracks, offsets: $trackDataOffsets")
        
        _playbackState.value = MultiTrackPlaybackState(
            isPlaying = false,
            positionMs = 0,
            trackMuted = trackMuted.toList()
        )
    }
    
    /**
     * Find the 'data' chunk offset in a WAV file by parsing RIFF structure.
     */
    private fun findWavDataOffset(file: RandomAccessFile): Long {
        try {
            file.seek(0)
            val buffer = ByteArray(12)
            file.read(buffer)
            
            // Verify RIFF header
            val riff = String(buffer, 0, 4)
            val wave = String(buffer, 8, 4)
            if (riff != "RIFF" || wave != "WAVE") {
                Log.w(TAG, "Not a valid WAV file")
                return 44L
            }
            
            // Search for 'data' chunk
            var offset = 12L
            val chunkHeader = ByteArray(8)
            
            while (offset < file.length() - 8) {
                file.seek(offset)
                file.read(chunkHeader)
                
                val chunkId = String(chunkHeader, 0, 4)
                val chunkSize = ByteBuffer.wrap(chunkHeader, 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                
                if (chunkId == "data") {
                    Log.d(TAG, "Found data chunk at offset ${offset + 8}, size: $chunkSize")
                    return offset + 8
                }
                
                offset += 8 + chunkSize
                // Pad to even boundary
                if (chunkSize % 2 != 0L) offset++
            }
            
            Log.w(TAG, "Data chunk not found, using default offset")
            return 44L
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WAV header", e)
            return 44L
        }
    }
    
    fun play() {
        when (playerState.get()) {
            PlayerState.STOPPED, PlayerState.PAUSED -> {
                playerState.set(PlayerState.PLAYING)
                playbackThread = Thread({ playAudio() }, "MultiTrackPlayer").apply {
                    priority = Thread.MAX_PRIORITY
                    start()
                }
                updateState()
            }
            PlayerState.PLAYING -> { /* Already playing */ }
        }
    }
    
    fun pause() {
        if (playerState.get() == PlayerState.PLAYING) {
            playerState.set(PlayerState.PAUSED)
            synchronized(playLock) {
                stopAudioTrack()
            }
            updateState()
        }
    }
    
    fun stop() {
        playerState.set(PlayerState.STOPPED)
        synchronized(playLock) {
            stopAudioTrack()
        }
        currentFramePosition.set(0)
        updateState()
    }
    
    fun seekToMs(positionMs: Long) {
        val framePosition = (positionMs * SAMPLE_RATE) / 1000
        seekToFrames(framePosition)
    }
    
    fun seekToSamples(positionSamples: Long) {
        seekToFrames(positionSamples)
    }
    
    private fun seekToFrames(framePosition: Long) {
        val wasPlaying = playerState.get() == PlayerState.PLAYING
        
        if (wasPlaying) {
            playerState.set(PlayerState.PAUSED)
            synchronized(playLock) {
                stopAudioTrack()
            }
        }
        
        currentFramePosition.set(framePosition)
        
        // Seek each file to the correct byte position
        val inputByteOffset = framePosition * INPUT_BYTES_PER_FRAME
        trackFiles.forEachIndexed { index, file ->
            file?.let {
                try {
                    val offset = trackDataOffsets[index] + inputByteOffset
                    it.seek(offset)
                } catch (e: Exception) {
                    Log.e(TAG, "Seek error for track $index", e)
                }
            }
        }
        
        if (wasPlaying) {
            playerState.set(PlayerState.PLAYING)
            playbackThread = Thread({ playAudio() }, "MultiTrackPlayer").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
        
        updateState()
    }
    
    fun toggleTrackMute(trackIndex: Int, muted: Boolean) {
        if (trackIndex in trackMuted.indices) {
            trackMuted[trackIndex] = muted
            updateState()
        }
    }
    
    private fun stopAudioTrack() {
        playbackThread?.interrupt()
        playbackThread = null
        
        audioTrack?.let {
            try {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        audioTrack = null
    }
    
    private fun buildAudioTrack(): AudioTrack {
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBufferSize)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
        } else {
            builder.build()
        }
    }
    
    private fun playAudio() {
        audioTrack = buildAudioTrack()
        audioTrack?.play()
        
        // Input buffers: 32-bit integer stereo (8 bytes per frame)
        val inputBufferSize = FRAMES_PER_BUFFER * INPUT_BYTES_PER_FRAME
        val inputBuffers = trackFiles.map { ByteArray(inputBufferSize) }.toTypedArray()
        
        // Output buffer: float array (stereo: 2 floats per frame)
        val outputFloatsPerBuffer = FRAMES_PER_BUFFER * OUTPUT_FLOATS_PER_FRAME
        val outputBuffer = FloatArray(outputFloatsPerBuffer)
        
        // Seek to current position
        val inputByteOffset = currentFramePosition.get() * INPUT_BYTES_PER_FRAME
        trackFiles.forEachIndexed { index, file ->
            file?.let {
                try {
                    val offset = trackDataOffsets[index] + inputByteOffset
                    it.seek(offset)
                } catch (e: Exception) { }
            }
        }
        
        synchronized(playLock) {
            try {
                while (playerState.get() == PlayerState.PLAYING) {
                    var maxFramesRead = 0
                    var allFinished = true
                    
                    // Read from all track files
                    for (i in trackFiles.indices) {
                        val file = trackFiles[i]
                        if (file != null) {
                            val bytesRead = file.read(inputBuffers[i], 0, inputBufferSize)
                            val framesRead = if (bytesRead > 0) bytesRead / INPUT_BYTES_PER_FRAME else 0
                            
                            if (framesRead > 0) {
                                allFinished = false
                                if (framesRead > maxFramesRead) {
                                    maxFramesRead = framesRead
                                }
                            }
                            
                            // Zero out remaining bytes
                            if (bytesRead < inputBufferSize) {
                                val start = if (bytesRead < 0) 0 else bytesRead
                                for (j in start until inputBufferSize) {
                                    inputBuffers[i][j] = 0
                                }
                            }
                        } else {
                            // No file - fill with silence
                            inputBuffers[i].fill(0)
                        }
                    }
                    
                    if (allFinished || maxFramesRead <= 0) {
                        break
                    }
                    
                    // Mix all tracks: convert 32-bit int to float and sum
                    mixInt32StereoToFloat(outputBuffer, inputBuffers, trackMuted, maxFramesRead)
                    
                    // Write to AudioTrack (float array)
                    val floatsToWrite = maxFramesRead * OUTPUT_FLOATS_PER_FRAME
                    audioTrack?.write(outputBuffer, 0, floatsToWrite, AudioTrack.WRITE_BLOCKING)
                    
                    // Update position
                    currentFramePosition.addAndGet(maxFramesRead.toLong())
                    updateState()
                }
            } catch (e: InterruptedException) {
                // Playback interrupted
            } catch (e: Exception) {
                Log.e(TAG, "Playback error", e)
            }
        }
        
        // Playback ended
        if (playerState.get() == PlayerState.PLAYING) {
            playerState.set(PlayerState.STOPPED)
            currentFramePosition.set(0)
        }
        stopAudioTrack()
        updateState()
    }
    
    /**
     * Convert 32-bit signed integer stereo to float stereo and mix tracks.
     * Input: 32-bit signed int per channel (-2147483648 to 2147483647)
     * Output: Float per channel (-1.0 to 1.0)
     */
    private fun mixInt32StereoToFloat(
        output: FloatArray,
        inputs: Array<ByteArray>,
        muted: Array<Boolean>,
        frameCount: Int
    ) {
        // Count active (non-muted) tracks for normalization
        val activeCount = muted.count { !it }.coerceAtLeast(1)
        val normalizationFactor = 1.0f / activeCount
        
        for (frame in 0 until frameCount) {
            var leftSum = 0f
            var rightSum = 0f
            
            for (trackIndex in inputs.indices) {
                if (muted.getOrElse(trackIndex) { true }) continue
                
                val inputOffset = frame * INPUT_BYTES_PER_FRAME
                
                // Read left channel as 32-bit signed integer (little-endian)
                val leftInt = ByteBuffer.wrap(inputs[trackIndex], inputOffset, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                
                // Read right channel as 32-bit signed integer (little-endian)
                val rightInt = ByteBuffer.wrap(inputs[trackIndex], inputOffset + 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                
                // Convert to float range (-1.0 to 1.0)
                leftSum += leftInt / INT32_MAX
                rightSum += rightInt / INT32_MAX
            }
            
            // Normalize to prevent clipping when mixing multiple tracks
            val outputOffset = frame * OUTPUT_FLOATS_PER_FRAME
            output[outputOffset] = (leftSum * normalizationFactor).coerceIn(-1f, 1f)
            output[outputOffset + 1] = (rightSum * normalizationFactor).coerceIn(-1f, 1f)
        }
    }
    
    private fun updateState() {
        _playbackState.value = MultiTrackPlaybackState(
            isPlaying = playerState.get() == PlayerState.PLAYING,
            positionMs = currentPositionMs,
            positionSamples = currentPositionSamples,
            trackMuted = trackMuted.toList()
        )
    }
    
    fun release() {
        stop()
        trackFiles.forEach { it?.close() }
        trackFiles = emptyList()
    }
}

data class MultiTrackPlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val positionSamples: Long = 0,
    val trackMuted: List<Boolean> = listOf(false, false, false, false)
)

enum class PlayerState {
    STOPPED,
    PLAYING,
    PAUSED
}
