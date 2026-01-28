package com.op1sync.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.op1sync.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tape deck visualization with animated reels.
 * Shows timer in the center between the reels.
 */
@Composable
fun TapeDeckImage(
    isPlaying: Boolean,
    currentTimeMs: Long,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reel_rotation")
    
    val leftReelAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "left_reel"
    )
    
    val rightReelAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "right_reel"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timer display - above the deck
        Text(
            text = formatTime(currentTimeMs),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp
            ),
            color = TeLightGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.2f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                
                val leftReelCenterX = canvasWidth * 0.28f
                val rightReelCenterX = canvasWidth * 0.72f
                val reelCenterY = canvasHeight * 0.40f
                val reelRadius = canvasHeight * 0.35f
                
                // Draw tape path (connecting line between reels)
                drawTapePath(
                    leftX = leftReelCenterX,
                    rightX = rightReelCenterX,
                    y = reelCenterY,
                    radius = reelRadius
                )
                
                // Draw left reel
                drawReel(
                    centerX = leftReelCenterX,
                    centerY = reelCenterY,
                    radius = reelRadius,
                    rotation = if (isPlaying) leftReelAngle else 0f
                )
                
                // Draw right reel
                drawReel(
                    centerX = rightReelCenterX,
                    centerY = reelCenterY,
                    radius = reelRadius,
                    rotation = if (isPlaying) rightReelAngle else 0f
                )
                
                // Draw tape guides and head
                drawTapeGuides(
                    leftX = leftReelCenterX,
                    rightX = rightReelCenterX,
                    reelY = reelCenterY,
                    reelRadius = reelRadius
                )
            }
        }
    }
}

private fun DrawScope.drawReel(
    centerX: Float,
    centerY: Float,
    radius: Float,
    rotation: Float
) {
    val strokeColor = Color(0xFF888888)
    val strokeWidth = 2.dp.toPx()
    
    // Outer circle
    drawCircle(
        color = strokeColor,
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = strokeWidth)
    )
    
    // Inner hub
    val hubRadius = radius * 0.35f
    drawCircle(
        color = strokeColor,
        radius = hubRadius,
        center = Offset(centerX, centerY),
        style = Stroke(width = strokeWidth)
    )
    
    // Center dot
    drawCircle(
        color = strokeColor,
        radius = radius * 0.08f,
        center = Offset(centerX, centerY),
        style = Stroke(width = strokeWidth)
    )
    
    // Spokes (rotate with animation)
    rotate(rotation, pivot = Offset(centerX, centerY)) {
        for (i in 0 until 3) {
            val angle = Math.toRadians((i * 120).toDouble())
            val startX = centerX + (hubRadius * 0.5f) * cos(angle).toFloat()
            val startY = centerY + (hubRadius * 0.5f) * sin(angle).toFloat()
            val endX = centerX + (radius * 0.85f) * cos(angle).toFloat()
            val endY = centerY + (radius * 0.85f) * sin(angle).toFloat()
            
            drawLine(
                color = strokeColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawTapePath(
    leftX: Float,
    rightX: Float,
    y: Float,
    radius: Float
) {
    val strokeColor = Color(0xFF666666)
    val strokeWidth = 1.5.dp.toPx()
    
    // Bottom tape path connecting the reels
    val tapeY = y + radius + 20.dp.toPx()
    drawLine(
        color = strokeColor,
        start = Offset(leftX, tapeY),
        end = Offset(rightX, tapeY),
        strokeWidth = strokeWidth
    )
}

private fun DrawScope.drawTapeGuides(
    leftX: Float,
    rightX: Float,
    reelY: Float,
    reelRadius: Float
) {
    val strokeColor = Color(0xFF666666)
    val strokeWidth = 1.5.dp.toPx()
    val guideRadius = 8.dp.toPx()
    
    val guideY = reelY + reelRadius + 20.dp.toPx()
    val centerX = (leftX + rightX) / 2
    
    // Left guide roller
    val leftGuideX = leftX + reelRadius * 0.3f
    drawCircle(
        color = strokeColor,
        radius = guideRadius,
        center = Offset(leftGuideX, guideY),
        style = Stroke(width = strokeWidth)
    )
    
    // Right guide roller
    val rightGuideX = rightX - reelRadius * 0.3f
    drawCircle(
        color = strokeColor,
        radius = guideRadius,
        center = Offset(rightGuideX, guideY),
        style = Stroke(width = strokeWidth)
    )
    
    // Tape head (center square)
    val headSize = 12.dp.toPx()
    drawRect(
        color = strokeColor,
        topLeft = Offset(centerX - headSize / 2, guideY - headSize / 2),
        size = androidx.compose.ui.geometry.Size(headSize, headSize),
        style = Stroke(width = strokeWidth)
    )
    
    // Lines from reels to guides
    drawLine(
        color = strokeColor,
        start = Offset(leftX, reelY + reelRadius),
        end = Offset(leftGuideX, guideY - guideRadius),
        strokeWidth = strokeWidth
    )
    
    drawLine(
        color = strokeColor,
        start = Offset(rightX, reelY + reelRadius),
        end = Offset(rightGuideX, guideY - guideRadius),
        strokeWidth = strokeWidth
    )
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val frames = ((timeMs % 1000) * 24 / 1000)
    return "%d:%02d:%02d".format(minutes, seconds, frames)
}
