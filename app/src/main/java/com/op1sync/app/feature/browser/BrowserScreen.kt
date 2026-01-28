package com.op1sync.app.feature.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.op1sync.app.ui.components.MiniPlayer
import com.op1sync.app.ui.theme.*
import kotlinx.coroutines.delay

data class FileItem(
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val handle: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateBack: () -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.audioPlayerManager.playbackState.collectAsState()
    
    // Update playback position periodically
    LaunchedEffect(playbackState.isPlaying) {
        while (playbackState.isPlaying) {
            delay(100)
            viewModel.audioPlayerManager.updatePosition()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PRZEGLĄDARKA",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.currentPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = TeMediumGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.canGoBack) {
                            viewModel.navigateUp()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Wstecz"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Odśwież"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TeBlack,
                    titleContentColor = TeLightGray
                )
            )
        },
        bottomBar = {
            MiniPlayer(
                playbackState = playbackState,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onCloseClick = { viewModel.stopPlayback() },
                onSeek = { /* TODO: Implement seek */ }
            )
        },
        containerColor = TeBlack
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TeOrange)
            }
        } else if (uiState.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOff,
                        contentDescription = null,
                        tint = TeMediumGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Brak plików",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TeMediumGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(uiState.items) { item ->
                    FileItemCard(
                        item = item,
                        isPlaying = playbackState.currentTrack?.name == item.name && playbackState.isPlaying,
                        onClick = {
                            if (item.isDirectory) {
                                viewModel.navigateToFolder(item)
                            } else if (isAudioFile(item.name)) {
                                viewModel.playFile(item)
                            }
                        },
                        onDownload = { viewModel.downloadFile(item) }
                    )
                }
                
                // Extra space for mini player
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun FileItemCard(
    item: FileItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val isAudio = isAudioFile(item.name)
    
    val icon: ImageVector = when {
        item.isDirectory -> Icons.Outlined.Folder
        isAudio -> if (isPlaying) Icons.Outlined.GraphicEq else Icons.Outlined.AudioFile
        item.name.endsWith(".json") -> Icons.Outlined.Description
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) TeOrange.copy(alpha = 0.15f) else TeDarkGray
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    isPlaying -> TeOrange
                    item.isDirectory -> TeOrange
                    isAudio -> TeGreen
                    else -> TeLightGray
                },
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPlaying) TeOrange else TeLightGray
                )
                Row {
                    if (!item.isDirectory && item.size > 0) {
                        Text(
                            text = formatFileSize(item.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = TeMediumGray
                        )
                    }
                    if (isAudio && !item.isDirectory) {
                        Text(
                            text = " • Kliknij aby odtworzyć",
                            style = MaterialTheme.typography.bodySmall,
                            color = TeMediumGray
                        )
                    }
                }
            }
            
            if (!item.isDirectory) {
                IconButton(onClick = onDownload) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Pobierz",
                        tint = TeOrange
                    )
                }
            }
        }
    }
}

private fun isAudioFile(name: String): Boolean {
    val lower = name.lowercase()
    return lower.endsWith(".wav") || lower.endsWith(".aif") || lower.endsWith(".aiff")
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
