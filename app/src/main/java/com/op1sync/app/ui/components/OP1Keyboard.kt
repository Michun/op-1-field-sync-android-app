package com.op1sync.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.op1sync.app.ui.theme.*

/**
 * OP-1 style keyboard for drum kit playback.
 * 24 keys arranged like OP-1: 14 main keys + 10 circular encoder keys
 * 
 * Layout matches OP-1 drum mapping:
 * F1-B1 (keys 0-6): Main drums row 1
 * C2-E2 (keys 7-11): Main drums row 2  
 * F2-B2 (keys 12-18): Percussion/Bass row 1
 * C3-E3 (keys 19-23): Bass row 2
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Keys 0-6 (F1, F#1, G1, G#1, A1, A#1, B1)
        KeyboardRow(
            keyIndices = listOf(0, 1, 2, 3, 4, 5, 6),
            sampleNames = sampleNames,
            onKeyPress = onKeyPress,
            colors = listOf(TeOrange, TeOrange, TeGreen, TeGreen, TeBlue, TeBlue, TeMediumGray)
        )
        
        // Row 2: Keys 7-11 (C2, C#2, D2, D#2, E2)
        KeyboardRow(
            keyIndices = listOf(7, 8, 9, 10, 11),
            sampleNames = sampleNames,
            onKeyPress = onKeyPress,
            colors = listOf(TeOrange, TeGreen, TeGreen, TeBlue, TeBlue),
            isShortRow = true
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Row 3: Keys 12-18
        KeyboardRow(
            keyIndices = listOf(12, 13, 14, 15, 16, 17, 18),
            sampleNames = sampleNames,
            onKeyPress = onKeyPress,
            colors = listOf(TeOrange, TeOrange, TeGreen, TeGreen, TeBlue, TeBlue, TeMediumGray)
        )
        
        // Row 4: Keys 19-23
        KeyboardRow(
            keyIndices = listOf(19, 20, 21, 22, 23),
            sampleNames = sampleNames,
            onKeyPress = onKeyPress,
            colors = listOf(TeOrange, TeGreen, TeGreen, TeBlue, TeBlue),
            isShortRow = true
        )
    }
}

@Composable
private fun KeyboardRow(
    keyIndices: List<Int>,
    sampleNames: List<String>,
    onKeyPress: (Int) -> Unit,
    colors: List<Color>,
    isShortRow: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isShortRow) 32.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        keyIndices.forEachIndexed { index, keyIndex ->
            val color = colors.getOrElse(index) { TeMediumGray }
            val name = sampleNames.getOrElse(keyIndex) { "" }
            
            DrumKey(
                label = name,
                color = color,
                onClick = { onKeyPress(keyIndex) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DrumKey(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor = if (isPressed) color else TeDarkGray
    val borderColor = if (isPressed) color else color.copy(alpha = 0.5f)
    val textColor = if (isPressed) TeBlack else TeLightGray
    
    Box(
        modifier = modifier
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp)
        )
    }
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
