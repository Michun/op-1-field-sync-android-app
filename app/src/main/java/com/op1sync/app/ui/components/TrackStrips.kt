package com.op1sync.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.op1sync.app.ui.theme.*

/**
 * Region of recorded audio on a track.
 * Start and end are in samples (44100 samples = 1 second).
 */
data class TrackRegion(
    val startSample: Long,
    val endSample: Long
)

/**
 * Four track strips showing recorded regions with mute toggles.
 */
@Composable
fun TrackStrips(
    trackRegions: List<List<TrackRegion>>,
    totalSamples: Long,
    currentPosition: Long,
    trackMuted: List<Boolean>,
    onTrackMuteToggle: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColors = listOf(
        listOf(Color(0xFFff3d3d), Color(0xFF1741b7), Color(0xFF2ae743)),  // Track 1: red, blue, green
        listOf(Color(0xFFff3d3d)),  // Track 2: red
        listOf(Color(0xFFff3d3d)),  // Track 3: red  
        listOf(Color(0xFFff3d3d))   // Track 4: red
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        trackRegions.forEachIndexed { trackIndex, regions ->
            TrackStrip(
                regions = regions,
                totalSamples = totalSamples,
                colors = trackColors.getOrElse(trackIndex) { listOf(Color(0xFFff3d3d)) },
                isMuted = trackMuted.getOrElse(trackIndex) { false },
                onMuteToggle = { onTrackMuteToggle(trackIndex, it) }
            )
        }
    }
}

@Composable
private fun TrackStrip(
    regions: List<TrackRegion>,
    totalSamples: Long,
    colors: List<Color>,
    isMuted: Boolean,
    onMuteToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Track bar with regions
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(TeMediumGray.copy(alpha = 0.3f))
        ) {
            if (totalSamples <= 0) return@Canvas
            
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            regions.forEachIndexed { index, region ->
                val startFraction = region.startSample.toFloat() / totalSamples
                val endFraction = region.endSample.toFloat() / totalSamples
                
                val startX = startFraction * canvasWidth
                val width = (endFraction - startFraction) * canvasWidth
                
                val color = colors[index % colors.size]
                
                drawRect(
                    color = if (isMuted) color.copy(alpha = 0.3f) else color,
                    topLeft = Offset(startX, 0f),
                    size = Size(width.coerceAtLeast(2f), canvasHeight)
                )
            }
        }
        
        // Mute toggle button
        IconButton(
            onClick = { onMuteToggle(!isMuted) },
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isMuted) TeMediumGray else TeBlack,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = if (isMuted) TeBlack else TeLightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Seek bar with minute markers.
 * Padded on right to align with track strips (mute button takes 32dp + 12dp spacing).
 */
@Composable
fun TapeSeekBar(
    currentPosition: Long,
    totalSamples: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSamples > 0) {
        (currentPosition.toFloat() / totalSamples).coerceIn(0f, 1f)
    } else 0f
    
    Column(modifier = modifier) {
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                onSeek((newProgress * totalSamples).toLong())
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 44.dp),  // Align with track strips (32dp button + 12dp spacing)
            colors = SliderDefaults.colors(
                thumbColor = TeLightGray,
                activeTrackColor = TeMediumGray,
                inactiveTrackColor = TeMediumGray.copy(alpha = 0.3f)
            )
        )
    }
}

/**
 * Playback control buttons: Play and Stop.
 * Styled like OP-1 hardware buttons with 3D effect.
 */
@Composable
fun TapeControls(
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause button
        Op1Button(
            onClick = if (isPlaying) onPause else onPlay,
            isActive = isPlaying
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = TeBlack,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(Modifier.width(32.dp))
        
        // Stop button
        Op1Button(
            onClick = onStop,
            isActive = false
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(TeBlack)
            )
        }
    }
}

/**
 * OP-1 style 3D button with shadow effect.
 */
@Composable
private fun Op1Button(
    onClick: () -> Unit,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer shadow/border
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = Color(0xFFB0B0B0),
            shadowElevation = 4.dp
        ) {
            // Inner button
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = if (isActive) Color(0xFFE8E8E8) else Color(0xFFF5F5F5),
                onClick = onClick
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        }
    }
}

