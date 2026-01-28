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
import com.op1sync.app.core.download.DownloadStatus
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
    val downloadState by viewModel.downloadManager.downloadState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Update playback position periodically
    LaunchedEffect(playbackState.isPlaying) {
        while (playbackState.isPlaying) {
            delay(100)
            viewModel.audioPlayerManager.updatePosition()
        }
    }
    
    // Show snackbar for messages
    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
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
            Column {
                // Folder download progress
                downloadState.folderDownloadProgress?.let { progress ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = TeDarkGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pobieranie: ${progress.folderName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TeLightGray
                                )
                                Text(
                                    text = "${progress.completedFiles}/${progress.totalFiles}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TeOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { 
                                    if (progress.totalFiles > 0) 
                                        progress.completedFiles.toFloat() / progress.totalFiles 
                                    else 0f 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = TeOrange,
                                trackColor = TeMediumGray,
                            )
                            if (progress.currentFile.isNotEmpty()) {
                                Text(
                                    text = progress.currentFile,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TeMediumGray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
                
                MiniPlayer(
                    playbackState = playbackState,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onCloseClick = { viewModel.stopPlayback() },
                    onSeek = { percent -> viewModel.seekTo(percent) }
                )
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = TeDarkGray,
                    contentColor = TeLightGray
                )
            }
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
                    val downloadItem = downloadState.activeDownloads[item.handle]
                    
                    FileItemCard(
                        item = item,
                        isPlaying = playbackState.currentTrack?.name == item.name && playbackState.isPlaying,
                        downloadStatus = downloadItem?.status,
                        downloadProgress = downloadItem?.progress ?: 0f,
                        isFolderDownloading = uiState.isFolderDownloading,
                        onClick = {
                            if (item.isDirectory) {
                                viewModel.navigateToFolder(item)
                            } else if (isAudioFile(item.name)) {
                                viewModel.playFile(item)
                            }
                        },
                        onDownload = { 
                            if (item.isDirectory) {
                                viewModel.downloadFolder(item)
                            } else {
                                viewModel.downloadFile(item)
                            }
                        }
                    )
                }
                
                // Extra space for mini player
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }
}

@Composable
private fun FileItemCard(
    item: FileItem,
    isPlaying: Boolean,
    downloadStatus: DownloadStatus?,
    downloadProgress: Float,
    isFolderDownloading: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    val isAudio = isAudioFile(item.name)
    val isDownloading = downloadStatus == DownloadStatus.InProgress
    val isDownloaded = downloadStatus == DownloadStatus.Completed
    
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
            containerColor = when {
                isPlaying -> TeOrange.copy(alpha = 0.15f)
                isDownloaded -> TeGreen.copy(alpha = 0.1f)
                else -> TeDarkGray
            }
        )
    ) {
        Column {
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
                        if (item.isDirectory) {
                            Text(
                                text = "Folder",
                                style = MaterialTheme.typography.bodySmall,
                                color = TeMediumGray
                            )
                        } else if (item.size > 0) {
                            Text(
                                text = formatFileSize(item.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = TeMediumGray
                            )
                        }
                        if (isDownloaded) {
                            Text(
                                text = " • Pobrano ✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = TeGreen
                            )
                        }
                    }
                }
                
                // Download button
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TeOrange,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onDownload,
                        enabled = !isFolderDownloading || !item.isDirectory
                    ) {
                        Icon(
                            imageVector = when {
                                isDownloaded -> Icons.Outlined.CheckCircle
                                item.isDirectory -> Icons.Outlined.FolderCopy
                                else -> Icons.Outlined.Download
                            },
                            contentDescription = if (item.isDirectory) "Pobierz folder" else "Pobierz",
                            tint = when {
                                isDownloaded -> TeGreen
                                isFolderDownloading && item.isDirectory -> TeMediumGray
                                else -> TeOrange
                            }
                        )
                    }
                }
            }
            
            // Download progress bar
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = TeOrange,
                    trackColor = TeMediumGray,
                )
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
