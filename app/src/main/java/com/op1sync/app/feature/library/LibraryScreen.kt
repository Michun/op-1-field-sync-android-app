package com.op1sync.app.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.op1sync.app.data.local.FileType
import com.op1sync.app.data.local.LocalFileEntity
import com.op1sync.app.data.local.ProjectFolder
import com.op1sync.app.ui.components.MiniPlayer
import com.op1sync.app.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.audioPlayerManager.playbackState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
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
                    Text(
                        text = "BIBLIOTEKA",
                        style = MaterialTheme.typography.titleLarge
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Szukaj projektu...", color = TeMediumGray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = TeMediumGray
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TeOrange,
                    unfocusedBorderColor = TeMediumGray,
                    focusedTextColor = TeLightGray,
                    unfocusedTextColor = TeLightGray,
                    cursorColor = TeOrange
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            // Filter chips
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FileTypeFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filterLabel(filter)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TeOrange,
                            selectedLabelColor = TeWhite,
                            containerColor = TeDarkGray,
                            labelColor = TeLightGray
                        )
                    )
                }
            }
            
            // Folder count
            Text(
                text = "${uiState.filteredFolders.size} projektów",
                style = MaterialTheme.typography.bodySmall,
                color = TeMediumGray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            // Folder list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TeOrange)
                }
            } else if (uiState.filteredFolders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.LibraryMusic,
                            contentDescription = null,
                            tint = TeMediumGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.folders.isEmpty()) 
                                "Biblioteka jest pusta\nPobierz projekty z OP-1" 
                            else "Brak wyników",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TeMediumGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredFolders) { folder ->
                        ProjectFolderCard(
                            folder = folder,
                            isExpanded = uiState.expandedFolder == folder.folderName,
                            isPlaying = playbackState.currentTrack?.let { track ->
                                folder.files.any { it.name == track.name }
                            } ?: false,
                            currentTrackName = playbackState.currentTrack?.name,
                            onToggleExpand = { viewModel.toggleFolderExpanded(folder) },
                            onPlay = { viewModel.playFirstInFolder(folder) },
                            onPlayFile = { viewModel.playFile(it) },
                            onFavorite = { viewModel.toggleFolderFavorite(folder) },
                            onDelete = { viewModel.deleteFolder(folder) }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProjectFolderCard(
    folder: ProjectFolder,
    isExpanded: Boolean,
    isPlaying: Boolean,
    currentTrackName: String?,
    onToggleExpand: () -> Unit,
    onPlay: () -> Unit,
    onPlayFile: (LocalFileEntity) -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Usuń projekt?") },
            text = { 
                Text("Czy na pewno chcesz usunąć ${folder.folderName}?\n(${folder.fileCount} plików)")
            },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Usuń", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Anuluj")
                }
            },
            containerColor = TeDarkGray,
            titleContentColor = TeLightGray,
            textContentColor = TeMediumGray
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) TeOrange.copy(alpha = 0.15f) else TeDarkGray
        )
    ) {
        Column {
            // Folder header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type icon
                Icon(
                    imageVector = when (folder.type) {
                        FileType.TAPE -> Icons.Outlined.Album
                        FileType.SYNTH -> Icons.Outlined.Piano
                        FileType.DRUM -> Icons.Outlined.Speaker
                        FileType.MIXDOWN -> Icons.Outlined.MusicNote
                        FileType.OTHER -> Icons.Outlined.Folder
                    },
                    contentDescription = null,
                    tint = if (isPlaying) TeOrange else TeGreen,
                    modifier = Modifier.size(36.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Folder info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.folderName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isPlaying) TeOrange else TeLightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row {
                        Text(
                            text = "${folder.fileCount} plików",
                            style = MaterialTheme.typography.bodySmall,
                            color = TeMediumGray
                        )
                        Text(
                            text = " • ${formatFileSize(folder.totalSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TeMediumGray
                        )
                        Text(
                            text = " • ${formatDate(folder.downloadedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TeMediumGray
                        )
                    }
                }
                
                // Play button
                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = "Odtwórz",
                        tint = TeOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Expand icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp 
                        else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                    tint = TeMediumGray
                )
            }
            
            // Expanded content - file list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(color = TeMediumGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // File list
                    folder.files.forEach { file ->
                        FileRow(
                            file = file,
                            isPlaying = file.name == currentTrackName,
                            onClick = { onPlayFile(file) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Actions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onFavorite) {
                            Icon(
                                imageVector = if (folder.isFavorite) Icons.Filled.Favorite 
                                    else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Ulubione",
                                tint = if (folder.isFavorite) TeOrange else TeMediumGray
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Usuń",
                                tint = TeMediumGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    file: LocalFileEntity,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val isAudio = file.name.lowercase().let {
        it.endsWith(".wav") || it.endsWith(".aif") || it.endsWith(".aiff")
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isAudio, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                isPlaying -> Icons.Outlined.GraphicEq
                isAudio -> Icons.Outlined.AudioFile
                file.name.endsWith(".json") -> Icons.Outlined.Description
                else -> Icons.Outlined.InsertDriveFile
            },
            contentDescription = null,
            tint = if (isPlaying) TeOrange else if (isAudio) TeGreen else TeMediumGray,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPlaying) TeOrange else if (isAudio) TeLightGray else TeMediumGray,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = formatFileSize(file.size),
            style = MaterialTheme.typography.bodySmall,
            color = TeMediumGray
        )
    }
}



private fun filterLabel(filter: FileTypeFilter): String = when (filter) {
    FileTypeFilter.ALL -> "Wszystko"
    FileTypeFilter.TAPES -> "Taśmy"
    FileTypeFilter.SYNTH -> "Synth"
    FileTypeFilter.DRUM -> "Drum"
    FileTypeFilter.MIXDOWN -> "Miks"
    FileTypeFilter.FAVORITES -> "Ulubione"
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
