package com.op1sync.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.op1sync.app.core.audio.PlaybackState
import com.op1sync.app.ui.theme.*

@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onExpandClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack
    
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(enabled = onExpandClick != null) { onExpandClick?.invoke() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TeDarkGray),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                // Waveform with seek
                WaveformView(
                    filePath = track?.uri?.path,
                    progress = if (playbackState.duration > 0) {
                        playbackState.position.toFloat() / playbackState.duration.toFloat()
                    } else 0f,
                    onSeek = onSeek,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Track info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = track?.name ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TeLightGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatTime(playbackState.position)} / ${formatTime(playbackState.duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TeMediumGray
                        )
                    }
                    
                    // Skip back 10s
                    IconButton(
                        onClick = { 
                            val newPos = (playbackState.position - 10000).coerceAtLeast(0)
                            onSeek(newPos.toFloat() / playbackState.duration.toFloat())
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "-10s",
                            tint = TeLightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Play/Pause button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TeOrange)
                            .clickable(onClick = onPlayPauseClick),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playbackState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = TeWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) 
                                    Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = TeWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Skip forward 10s
                    IconButton(
                        onClick = { 
                            val newPos = (playbackState.position + 10000).coerceAtMost(playbackState.duration)
                            onSeek(newPos.toFloat() / playbackState.duration.toFloat())
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "+10s",
                            tint = TeLightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Close button
                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = TeMediumGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
