package com.op1sync.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// OP-1 keyboard colors
private val KeyBackground = Color(0xFFF5F5F5)
private val KeyBorder = Color(0xFFE0E0E0)
private val BlackDot = Color(0xFF000000)
private val CapsuleShadow = Color(0xFFD0D0D0)
private val CapsuleColor = Color(0xFFE8E8E8)
private val PressedColor = Color(0xFFD8D8D8)

/**
 * OP-1 style keyboard for drum kit playback.
 * Matches the physical OP-1 keyboard layout with white keys and black dot markers.
 * 
 * Layout: 4 rows (2 octaves)
 * Each octave has: 7 white keys (bottom) with 5 black key positions (top)
 * Black key pattern: [dot, dot, dot, empty, dot, dot]
 */
@Composable
fun OP1Keyboard(
    sampleNames: List<String> = List(24) { "Sample ${it + 1}" },
    onKeyPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Octave 1 (keys 0-11)
        OP1Octave(
            whiteKeyIndices = listOf(0, 2, 4, 5, 7, 9, 11),
            blackKeyIndices = listOf(1, 3, -1, 6, 8, 10),  // -1 = empty slot
            onKeyPress = onKeyPress
        )
        
        // Octave 2 (keys 12-23)
        OP1Octave(
            whiteKeyIndices = listOf(12, 14, 16, 17, 19, 21, 23),
            blackKeyIndices = listOf(13, 15, -1, 18, 20, 22),
            onKeyPress = onKeyPress
        )
    }
}

/**
 * One octave of the OP-1 keyboard: 
 * Top row: 5 black key positions in white frames (with dots)
 * Bottom row: 7 white keys
 */
@Composable
private fun OP1Octave(
    whiteKeyIndices: List<Int>,
    blackKeyIndices: List<Int>,
    onKeyPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Top row: Black keys in white frames (offset by half key width)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            blackKeyIndices.forEach { keyIndex ->
                if (keyIndex >= 0) {
                    BlackKey(
                        onClick = { onKeyPress(keyIndex) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    // Empty slot (no black key between E-F)
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        // Bottom row: White keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            whiteKeyIndices.forEach { keyIndex ->
                WhiteKey(
                    onClick = { onKeyPress(keyIndex) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * White key with inner capsule/pastille and shadow effect.
 */
@Composable
private fun WhiteKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .aspectRatio(0.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) PressedColor else KeyBackground)
            .border(1.dp, KeyBorder, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner capsule
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .fillMaxHeight(0.55f)
        ) {
            drawCapsule(
                color = if (isPressed) PressedColor else CapsuleColor,
                shadowColor = CapsuleShadow
            )
        }
    }
}

/**
 * Black key in white frame with circular dot inside.
 */
@Composable
private fun BlackKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // White frame (same style as white keys but shorter)
    Box(
        modifier = modifier
            .aspectRatio(1.2f)  // Shorter than white keys
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) PressedColor else KeyBackground)
            .border(1.dp, KeyBorder, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Black dot in center
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isPressed) Color(0xFF333333) else BlackDot)
        )
    }
}

/**
 * Draw a vertical capsule (rounded rectangle) with shadow effect.
 */
private fun DrawScope.drawCapsule(
    color: Color,
    shadowColor: Color
) {
    val cornerRadius = size.width / 2
    
    // Shadow (offset down-right)
    drawRoundRect(
        color = shadowColor,
        topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
    
    // Main capsule
    drawRoundRect(
        color = color,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
}

/**
 * Default drum sample names matching OP-1 layout.
 */
object DrumSampleNames {
    val defaults = listOf(
        "Kick",      // 0
        "Kick Alt",  // 1
        "Snare",     // 2
        "Snare Alt", // 3
        "Rim",       // 4
        "Clap",      // 5
        "Tamb",      // 6
        "HH Close",  // 7
        "08",        // 8
        "HH Open",   // 9
        "10",        // 10
        "11",        // 11
        "Ride",      // 12
        "13",        // 13
        "Crash",     // 14
        "15",        // 15
        "?",         // 16
        "17",        // 17
        "Bass 1",    // 18
        "Bass 2",    // 19
        "Bass 3",    // 20
        "Bass 4",    // 21
        "Bass 5",    // 22
        "Bass 6"     // 23
    )
}
