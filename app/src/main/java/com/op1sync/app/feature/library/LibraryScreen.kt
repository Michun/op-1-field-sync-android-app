package com.op1sync.app.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.op1sync.app.data.local.FileType
import com.op1sync.app.data.local.LocalFileEntity
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
    
    // Update playback position
    LaunchedEffect(playbackState.isPlaying) {
        while (playbackState.isPlaying) {
            delay(100)
            viewModel.audioPlayerManager.updatePosition()
        }
    }
    
    // Show messages
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
                placeholder = { Text("Szukaj...", color = TeMediumGray) },
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
            
            // File count
            Text(
                text = "${uiState.filteredFiles.size} plików",
                style = MaterialTheme.typography.bodySmall,
                color = TeMediumGray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            // File list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TeOrange)
                }
            } else if (uiState.filteredFiles.isEmpty()) {
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
                            text = if (uiState.files.isEmpty()) 
                                "Biblioteka jest pusta\nPobierz pliki z OP-1" 
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
                    items(uiState.filteredFiles) { file ->
                        LibraryFileCard(
                            file = file,
                            isPlaying = playbackState.currentTrack?.name == file.name,
                            onPlay = { viewModel.playFile(file) },
                            onFavorite = { viewModel.toggleFavorite(file) },
                            onDelete = { viewModel.deleteFile(file) }
                        )
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LibraryFileCard(
    file: LocalFileEntity,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Usuń plik?") },
            text = { Text("Czy na pewno chcesz usunąć ${file.name}?") },
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
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
            // Type icon
            Icon(
                imageVector = when (file.type) {
                    FileType.TAPE -> Icons.Outlined.Album
                    FileType.SYNTH -> Icons.Outlined.Piano
                    FileType.DRUM -> Icons.Outlined.Speaker
                    FileType.MIXDOWN -> Icons.Outlined.MusicNote
                    FileType.OTHER -> Icons.Outlined.AudioFile
                },
                contentDescription = null,
                tint = if (isPlaying) TeOrange else TeGreen,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPlaying) TeOrange else TeLightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        text = formatFileSize(file.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TeMediumGray
                    )
                    Text(
                        text = " • ${formatDate(file.downloadedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TeMediumGray
                    )
                }
            }
            
            // Favorite button
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (file.isFavorite) Icons.Filled.Favorite 
                        else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Ulubione",
                    tint = if (file.isFavorite) TeOrange else TeMediumGray
                )
            }
            
            // Delete button
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
    else -> "${bytes / (1024 * 1024)} MB"
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
