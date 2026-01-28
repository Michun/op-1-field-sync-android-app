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
import com.op1sync.app.data.local.ProjectFolder
import com.op1sync.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tapes screen - displays list of tape projects.
 * Clicking a project navigates to TapeProjectScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProject: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Filter to tapes only
    LaunchedEffect(Unit) {
        viewModel.setFilter(FileTypeFilter.TAPES)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    TapeProjectListItem(
                        folder = folder,
                        onClick = { onNavigateToProject(folder.folderPath) },
                        onFavorite = { viewModel.toggleFolderFavorite(folder) },
                        onDelete = { viewModel.deleteFolder(folder) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TapeProjectListItem(
    folder: ProjectFolder,
    onClick: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TeDarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album icon
            Icon(
                imageVector = Icons.Outlined.Album,
                contentDescription = null,
                tint = TeOrange,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(Modifier.width(16.dp))
            
            // Project info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TeLightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.fileCount} plików • ${formatSize(folder.totalSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TeMediumGray
                )
            }
            
            // Favorite button
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (folder.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Ulubione",
                    tint = if (folder.isFavorite) TeOrange else TeMediumGray
                )
            }
            
            // Arrow indicating navigation
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TeMediumGray,
                modifier = Modifier.size(24.dp)
            )
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
