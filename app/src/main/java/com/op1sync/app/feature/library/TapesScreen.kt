package com.op1sync.app.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.op1sync.app.data.local.FileType
import com.op1sync.app.data.local.ProjectFolder
import com.op1sync.app.ui.components.MiniPlayer
import com.op1sync.app.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tapes screen - displays tape projects with track-based view.
 * Each tape is a multi-track project with multiple .aif files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapesScreen(
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.audioPlayerManager.playbackState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Filter to tapes only
    LaunchedEffect(Unit) {
        viewModel.setFilter(FileTypeFilter.TAPES)
    }
    
    LaunchedEffect(playbackState.isPlaying) {
        while (playbackState.isPlaying) {
            delay(100)
            viewModel.audioPlayerManager.updatePosition()
        }
    }
    
    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Album,
                            contentDescription = null,
                            tint = TeOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "TAŚMY",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Wstecz"
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
                onSeek = { viewModel.seekTo(it) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = TeBlack
    ) { paddingValues ->
        val tapeFolders = uiState.filteredFolders
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TeOrange)
            }
        } else if (tapeFolders.isEmpty()) {
            EmptyLibraryView(
                icon = Icons.Outlined.Album,
                message = "Brak pobranych taśm\nPobierz projekty z OP-1",
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${tapeFolders.size} projektów",
                        style = MaterialTheme.typography.bodySmall,
                        color = TeMediumGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(tapeFolders) { folder ->
                    TapeProjectCard(
                        folder = folder,
                        isPlaying = playbackState.currentTrack?.let { track ->
                            folder.files.any { it.name == track.name }
                        } ?: false,
                        currentTrackName = playbackState.currentTrack?.name,
                        onPlay = { viewModel.playFirstInFolder(folder) },
                        onPlayTrack = { viewModel.playFile(it) },
                        onFavorite = { viewModel.toggleFolderFavorite(folder) },
                        onDelete = { viewModel.deleteFolder(folder) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun TapeProjectCard(
    folder: ProjectFolder,
    isPlaying: Boolean,
    currentTrackName: String?,
    onPlay: () -> Unit,
    onPlayTrack: (com.op1sync.app.data.local.LocalFileEntity) -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Usuń taśmę?") },
            text = { Text("${folder.folderName}\n(${folder.fileCount} plików)") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Anuluj") } },
            containerColor = TeDarkGray
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) TeOrange.copy(alpha = 0.1f) else TeDarkGray
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.folderName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isPlaying) TeOrange else TeLightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.fileCount} ścieżek • ${formatSize(folder.totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TeMediumGray
                    )
                }
                
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (folder.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Ulubione",
                        tint = if (folder.isFavorite) TeOrange else TeMediumGray
                    )
                }
                
                FilledIconButton(
                    onClick = onPlay,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = TeOrange)
                ) {
                    Icon(Icons.Outlined.PlayArrow, "Odtwórz", tint = TeWhite)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = TeMediumGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))
            
            // Track list (only audio files)
            val audioTracks = folder.files.filter { 
                it.name.lowercase().let { n -> n.endsWith(".aif") || n.endsWith(".wav") }
            }
            
            audioTracks.forEachIndexed { index, track ->
                val isTrackPlaying = track.name == currentTrackName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayTrack(track) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isTrackPlaying) TeOrange else TeMediumGray,
                        modifier = Modifier.width(24.dp)
                    )
                    Icon(
                        imageVector = if (isTrackPlaying) Icons.Outlined.GraphicEq else Icons.Outlined.AudioFile,
                        contentDescription = null,
                        tint = if (isTrackPlaying) TeOrange else TeGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = track.name.removeSuffix(".aif").removeSuffix(".wav"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isTrackPlaying) TeOrange else TeLightGray,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatSize(track.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TeMediumGray
                    )
                }
            }
            
            // Delete button
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Outlined.Delete, null, tint = TeMediumGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Usuń", color = TeMediumGray, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryView(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = TeMediumGray, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = TeMediumGray)
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
