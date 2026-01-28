package com.op1sync.app.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Player for OP-1 drum samples using AudioTrack.
 * Plays raw 16-bit PCM mono data at 44100 Hz.
 */
@Singleton
class DrumSamplePlayer @Inject constructor() {
    
    companion object {
        private const val TAG = "DrumSamplePlayer"
        private const val SAMPLE_RATE = 44100
        private const val SAMPLE_CONVERSION = 4058 // From op1buddy - bytes per sample unit
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    
    private val minBufferSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)
    
    private var audioTrack: AudioTrack? = null
    private var loadedSamples: List<ByteArray> = emptyList()
    
    init {
        createAudioTrack()
    }
    
    @Synchronized
    private fun createAudioTrack() {
        try {
            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBufferSize)
            
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
            } else {
                builder.build()
            }
            audioTrack?.play()
            Log.d(TAG, "AudioTrack created, buffer size: $minBufferSize")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioTrack", e)
        }
    }
    
    /**
     * Load samples from raw SSND data and metadata start/end positions.
     */
    fun loadSamples(ssndData: ByteArray, startPositions: List<Int>, endPositions: List<Int>) {
        val samples = mutableListOf<ByteArray>()
        
        for (i in startPositions.indices) {
            if (i < endPositions.size) {
                val start = startPositions[i]
                val end = endPositions[i]
                
                // Convert sample positions to byte positions
                val byteStart = (start / SAMPLE_CONVERSION) * 2
                val byteEnd = (end / SAMPLE_CONVERSION) * 2
                
                if (byteStart >= 0 && byteEnd <= ssndData.size && byteStart < byteEnd) {
                    val sample = ssndData.copyOfRange(byteStart, byteEnd)
                    samples.add(sample)
                    Log.d(TAG, "Loaded sample $i: ${sample.size} bytes (start=$start end=$end)")
                } else {
                    samples.add(ByteArray(0))
                    Log.w(TAG, "Invalid sample range for index $i: $byteStart-$byteEnd (data size: ${ssndData.size})")
                }
            }
        }
        
        loadedSamples = samples
        Log.d(TAG, "Loaded ${samples.size} samples total")
    }
    
    /**
     * Play a sample by index.
     */
    fun play(sampleIndex: Int) {
        if (sampleIndex !in loadedSamples.indices) {
            Log.w(TAG, "Sample index out of range: $sampleIndex")
            return
        }
        
        val sample = loadedSamples[sampleIndex]
        if (sample.isEmpty()) {
            Log.w(TAG, "Sample $sampleIndex is empty")
            return
        }
        
        scope.launch {
            mutex.withLock {
                try {
                    val track = audioTrack ?: run {
                        Log.e(TAG, "AudioTrack is null")
                        return@withLock
                    }
                    
                    // Flush any pending audio
                    track.flush()
                    
                    // Write sample data in chunks to avoid large writes
                    var offset = 0
                    while (offset < sample.size) {
                        val chunkSize = minOf(minBufferSize, sample.size - offset)
                        val written = track.write(sample, offset, chunkSize)
                        if (written < 0) {
                            Log.e(TAG, "AudioTrack write error: $written")
                            break
                        }
                        offset += written
                    }
                    
                    Log.d(TAG, "Played sample $sampleIndex: ${sample.size} bytes")
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing sample $sampleIndex", e)
                }
            }
        }
    }
    
    /**
     * Get number of loaded samples.
     */
    fun getSampleCount(): Int = loadedSamples.size
    
    /**
     * Release resources.
     */
    @Synchronized
    fun release() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }
}

