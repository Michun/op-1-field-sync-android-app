package com.op1sync.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.op1sync.app.core.audio.*
import com.op1sync.app.ui.theme.*

/**
 * Dialog showing detailed patch metadata (info only, no playback).
 */
@Composable
fun PatchInfoDialog(
    metadata: OP1PatchMetadata,
    fileName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TeDarkGray)
        ) {
            Column(Modifier.padding(20.dp)) {
                // Header with icon and name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = getPatchTypeColor(metadata.engineType).copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            getPatchTypeIcon(metadata.engineType),
                            contentDescription = null,
                            tint = getPatchTypeColor(metadata.engineType),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(Modifier.weight(1f)) {
                        Text(
                            metadata.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TeWhite
                        )
                        Text(
                            OP1TypeNames.getEngineName(metadata.engineType),
                            style = MaterialTheme.typography.bodySmall,
                            color = getPatchTypeColor(metadata.engineType)
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Metadata details
                when (metadata) {
                    is DrumPatchMetadata -> DrumPatchDetails(metadata)
                    is SynthPatchMetadata -> SynthPatchDetails(metadata)
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Close button only (no playback for patches)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TeOrange)
                ) {
                    Text("Zamknij")
                }
            }
        }
    }
}

@Composable
private fun DrumPatchDetails(metadata: DrumPatchMetadata) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetadataRow("Sample", "${metadata.sampleCount} slotów")
        MetadataRow("Stereo", if (metadata.stereo) "Tak" else "Mono")
        MetadataRow("FX", if (metadata.fxActive) OP1TypeNames.getEffectName(metadata.fxType) else "Wyłączone")
        MetadataRow("LFO", if (metadata.lfoActive) OP1TypeNames.getLfoName(metadata.lfoType) else "Wyłączone")
    }
}

@Composable
private fun SynthPatchDetails(metadata: SynthPatchMetadata) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetadataRow("Engine", OP1TypeNames.getEngineName(metadata.type))
        MetadataRow("Oktawa", when {
            metadata.octave > 0 -> "+${metadata.octave}"
            metadata.octave < 0 -> "${metadata.octave}"
            else -> "0"
        })
        MetadataRow("FX", if (metadata.fxActive) OP1TypeNames.getEffectName(metadata.fxType) else "Wyłączone")
        MetadataRow("LFO", if (metadata.lfoActive) OP1TypeNames.getLfoName(metadata.lfoType) else "Wyłączone")
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TeMediumGray)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TeLightGray)
    }
}

/**
 * Get icon for patch type.
 */
fun getPatchTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "drum" -> Icons.Outlined.Sensors
    "drwave" -> Icons.Outlined.Waves
    "dbox" -> Icons.Outlined.Speaker
    "string" -> Icons.Outlined.MusicNote
    "cluster" -> Icons.Outlined.Grain
    "sampler" -> Icons.Outlined.GraphicEq
    "digital" -> Icons.Outlined.Memory
    "fm" -> Icons.Outlined.Radio
    "phase" -> Icons.Outlined.Autorenew
    "pulse" -> Icons.Outlined.Timeline
    "dsynth" -> Icons.Outlined.Tune
    "voltage" -> Icons.Outlined.ElectricBolt
    else -> Icons.Outlined.AudioFile
}

/**
 * Get color for patch type.
 */
fun getPatchTypeColor(type: String) = when (type.lowercase()) {
    "drum" -> TeOrange
    "drwave" -> TeBlue
    "dbox" -> TePurple
    "string" -> TeGreen
    "cluster" -> TePink
    "sampler" -> TeCyan
    "digital" -> TeYellow
    else -> TeGreen
}
