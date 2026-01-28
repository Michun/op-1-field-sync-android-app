package com.op1sync.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.op1sync.app.ui.theme.TeDarkGray
import com.op1sync.app.ui.theme.TeMediumGray
import com.op1sync.app.ui.theme.TeOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.max

/**
 * Waveform visualization component for audio files.
 * Displays amplitude bars with playback position indicator.
 */
@Composable
fun WaveformView(
    filePath: String?,
    progress: Float, // 0.0 to 1.0
    onSeek: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    playedColor: Color = TeOrange,
    unplayedColor: Color = TeMediumGray,
    backgroundColor: Color = TeDarkGray
) {
    var waveformData by remember { mutableStateOf<List<Float>>(emptyList()) }
    
    // Load waveform data when file changes
    LaunchedEffect(filePath) {
        if (filePath != null) {
            waveformData = withContext(Dispatchers.IO) {
                extractWaveformData(filePath)
            }
        } else {
            waveformData = emptyList()
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(onSeek) {
                if (onSeek != null) {
                    detectTapGestures { offset ->
                        val seekPosition = offset.x / size.width
                        onSeek(seekPosition.coerceIn(0f, 1f))
                    }
                }
            }
    ) {
        // Background
        drawRect(backgroundColor)
        
        if (waveformData.isEmpty()) {
            // Draw placeholder bars
            drawPlaceholderWaveform(unplayedColor)
        } else {
            // Draw actual waveform
            drawWaveformBars(waveformData, progress, playedColor, unplayedColor)
        }
        
        // Draw position indicator line
        if (progress > 0f) {
            val indicatorX = size.width * progress
            drawLine(
                color = playedColor,
                start = Offset(indicatorX, 0f),
                end = Offset(indicatorX, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawPlaceholderWaveform(color: Color) {
    val barCount = 50
    val barWidth = size.width / barCount * 0.7f
    val gap = size.width / barCount * 0.3f
    val centerY = size.height / 2
    
    for (i in 0 until barCount) {
        val x = i * (barWidth + gap) + gap / 2
        // Create a pattern that looks like audio waveform
        val amplitude = (0.2f + 0.3f * kotlin.math.sin(i * 0.5f).toFloat().let { abs(it) })
        val barHeight = size.height * amplitude
        
        drawRect(
            color = color.copy(alpha = 0.3f),
            topLeft = Offset(x, centerY - barHeight / 2),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawWaveformBars(
    data: List<Float>,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color
) {
    if (data.isEmpty()) return
    
    val barCount = data.size
    val barWidth = size.width / barCount * 0.7f
    val gap = size.width / barCount * 0.3f
    val centerY = size.height / 2
    val maxBarHeight = size.height * 0.9f
    
    val progressIndex = (progress * barCount).toInt()
    
    data.forEachIndexed { index, amplitude ->
        val x = index * (barWidth + gap) + gap / 2
        val barHeight = maxBarHeight * amplitude.coerceIn(0.05f, 1f)
        
        val color = if (index <= progressIndex) playedColor else unplayedColor
        
        drawRect(
            color = color,
            topLeft = Offset(x, centerY - barHeight / 2),
            size = Size(barWidth, barHeight)
        )
    }
}

/**
 * Extract waveform amplitude data from a WAV file.
 * Returns a list of normalized amplitude values (0.0 to 1.0).
 */
private fun extractWaveformData(filePath: String, sampleCount: Int = 100): List<Float> {
    return try {
        val file = File(filePath)
        if (!file.exists()) return emptyList()
        
        RandomAccessFile(file, "r").use { raf ->
            // Read WAV header
            val header = ByteArray(44)
            raf.read(header)
            
            // Verify it's a WAV file
            val riff = String(header, 0, 4)
            if (riff != "RIFF") return emptyList()
            
            // Get audio format info
            val channels = ((header[23].toInt() and 0xFF) shl 8) or (header[22].toInt() and 0xFF)
            val bitsPerSample = ((header[35].toInt() and 0xFF) shl 8) or (header[34].toInt() and 0xFF)
            val bytesPerSample = bitsPerSample / 8
            
            // Calculate data size
            val dataSize = raf.length() - 44
            val totalSamples = dataSize / (channels * bytesPerSample)
            val samplesPerBucket = (totalSamples / sampleCount).toInt().coerceAtLeast(1)
            
            val amplitudes = mutableListOf<Float>()
            var maxAmplitude = 0f
            
            // Sample the audio data
            for (i in 0 until sampleCount) {
                val position = 44L + (i * samplesPerBucket * channels * bytesPerSample)
                if (position >= raf.length()) break
                
                raf.seek(position)
                
                // Read samples and find max amplitude in this bucket
                var bucketMax = 0f
                val bucketSamples = minOf(samplesPerBucket, 100) // Limit reads per bucket
                
                for (j in 0 until bucketSamples) {
                    val sampleBytes = ByteArray(bytesPerSample)
                    if (raf.read(sampleBytes) != bytesPerSample) break
                    
                    val sample = when (bytesPerSample) {
                        1 -> (sampleBytes[0].toInt() and 0xFF) - 128
                        2 -> (sampleBytes[1].toInt() shl 8) or (sampleBytes[0].toInt() and 0xFF)
                        else -> 0
                    }
                    
                    bucketMax = max(bucketMax, abs(sample.toFloat()))
                }
                
                amplitudes.add(bucketMax)
                maxAmplitude = max(maxAmplitude, bucketMax)
            }
            
            // Normalize amplitudes
            if (maxAmplitude > 0) {
                amplitudes.map { it / maxAmplitude }
            } else {
                amplitudes.map { 0.5f }
            }
        }
    } catch (e: Exception) {
        // Return empty list on any error
        emptyList()
    }
}
