package com.op1sync.app.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.op1sync.app.core.audio.OP1PatchMetadata
import com.op1sync.app.data.local.FileType
import com.op1sync.app.data.local.LocalFileEntity
import com.op1sync.app.data.local.ProjectFolder
import com.op1sync.app.ui.components.MiniPlayer
import com.op1sync.app.ui.components.PatchInfoDialog
import com.op1sync.app.ui.components.getPatchTypeIcon
import com.op1sync.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Drum screen with category-based navigation.
 * Shows category tiles (slots, user, then others), drilling down to file lists.
 * Patches show metadata only (no playback).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumScreen(
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Current category being viewed (null = show category tiles)
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.setFilter(FileTypeFilter.DRUM)
    }
    
    // Group folders by their subcategory (second level folder name)
    val categories = remember(uiState.filteredFolders) {
        groupFoldersByCategory(uiState.filteredFolders)
    }
    
    // Show patch info dialog (info only, no playback for patches)
    uiState.selectedPatchMetadata?.let { metadata ->
        PatchInfoDialog(
            metadata = metadata,
            fileName = uiState.selectedFile?.name ?: "",
            onDismiss = { viewModel.dismissPatchMetadata() }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Sensors, null, tint = TeOrange, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedCategory?.uppercase() ?: "DRUM",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCategory != null) {
                            selectedCategory = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TeBlack, titleContentColor = TeLightGray)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = TeBlack
    ) { paddingValues ->
        
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
                CircularProgressIndicator(color = TeOrange)
            }
        } else if (categories.isEmpty()) {
            EmptyLibraryView(Icons.Outlined.Sensors, "Brak drum kitów\nPobierz z OP-1", Modifier.fillMaxSize().padding(paddingValues))
        } else {
            AnimatedContent(
                targetState = selectedCategory,
                label = "category_navigation"
            ) { category ->
                if (category == null) {
                    // Show category tiles
                    CategoryTilesView(
                        categories = categories,
                        icon = Icons.Outlined.Sensors,
                        onCategoryClick = { selectedCategory = it },
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                    )
                } else {
                    // Show files in selected category (info only)
                    PatchFilesView(
                        categoryName = category,
                        folders = categories[category] ?: emptyList(),
                        isSlots = category.equals("slots", ignoreCase = true),
                        onFileClick = { file ->
                            viewModel.showPatchMetadata(file)
                        },
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                    )
                }
            }
        }
    }
}

/**
 * Synth screen with category-based navigation.
 * Patches show metadata only (no playback).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthScreen(
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.setFilter(FileTypeFilter.SYNTH)
    }
    
    val categories = remember(uiState.filteredFolders) {
        groupFoldersByCategory(uiState.filteredFolders)
    }
    
    // Show patch info dialog (info only, no playback for patches)
    uiState.selectedPatchMetadata?.let { metadata ->
        PatchInfoDialog(
            metadata = metadata,
            fileName = uiState.selectedFile?.name ?: "",
            onDismiss = { viewModel.dismissPatchMetadata() }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Piano, null, tint = TeOrange, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedCategory?.uppercase() ?: "SYNTH",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCategory != null) {
                            selectedCategory = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TeBlack, titleContentColor = TeLightGray)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = TeBlack
    ) { paddingValues ->
        
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
                CircularProgressIndicator(color = TeOrange)
            }
        } else if (categories.isEmpty()) {
            EmptyLibraryView(Icons.Outlined.Piano, "Brak synth patches\nPobierz z OP-1", Modifier.fillMaxSize().padding(paddingValues))
        } else {
            AnimatedContent(
                targetState = selectedCategory,
                label = "category_navigation"
            ) { category ->
                if (category == null) {
                    CategoryTilesView(
                        categories = categories,
                        icon = Icons.Outlined.Piano,
                        onCategoryClick = { selectedCategory = it },
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                    )
                } else {
                    PatchFilesView(
                        categoryName = category,
                        folders = categories[category] ?: emptyList(),
                        isSlots = category.equals("slots", ignoreCase = true),
                        onFileClick = { file ->
                            viewModel.showPatchMetadata(file)
                        },
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                    )
                }
            }
        }
    }
}

/**
 * Grid view of category tiles.
 */
@Composable
private fun CategoryTilesView(
    categories: Map<String, List<ProjectFolder>>,
    icon: ImageVector,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Sort: slots first, then user, then alphabetically
    val sortedCategories = remember(categories) {
        categories.keys.sortedWith(compareBy(
            { if (it.equals("slots", true)) 0 else if (it.equals("user", true)) 1 else 2 },
            { it.lowercase() }
        ))
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(sortedCategories) { categoryName ->
            val folders = categories[categoryName] ?: emptyList()
            val fileCount = folders.sumOf { it.fileCount }
            
            CategoryTile(
                name = categoryName,
                icon = when {
                    categoryName.equals("slots", true) -> Icons.Outlined.GridView
                    categoryName.equals("user", true) -> Icons.Outlined.Person
                    else -> icon
                },
                fileCount = fileCount,
                onClick = { onCategoryClick(categoryName) }
            )
        }
    }
}

@Composable
private fun CategoryTile(
    name: String,
    icon: ImageVector,
    fileCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TeDarkGray)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TeGreen,
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TeLightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$fileCount plików",
                    style = MaterialTheme.typography.bodySmall,
                    color = TeMediumGray
                )
            }
        }
    }
}

/**
 * List view of patch files (info only, no playback).
 */
@Composable
private fun PatchFilesView(
    categoryName: String,
    folders: List<ProjectFolder>,
    isSlots: Boolean,
    onFileClick: (LocalFileEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allFiles = remember(folders) {
        folders.flatMap { it.files }
            .filter { it.name.lowercase().endsWith(".aif") }
            .sortedBy { it.name }
    }
    
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (isSlots) {
            // Special slots view: group by slot number
            val slotFiles = allFiles.groupBy { file ->
                val parts = file.sourcePath.split("/")
                val slotsIndex = parts.indexOfFirst { it.equals("slots", true) }
                if (slotsIndex >= 0 && slotsIndex + 1 < parts.size) {
                    parts[slotsIndex + 1].toIntOrNull() ?: 0
                } else 0
            }.toSortedMap()
            
            slotFiles.forEach { (slotNum, files) ->
                files.forEach { file ->
                    item {
                        PatchSlotRow(
                            slotNumber = slotNum,
                            file = file,
                            onClick = { onFileClick(file) }
                        )
                    }
                }
            }
        } else {
            // Regular file list
            items(allFiles) { file ->
                PatchFileRow(
                    file = file,
                    onClick = { onFileClick(file) }
                )
            }
        }
        
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PatchSlotRow(
    slotNumber: Int,
    file: LocalFileEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TeDarkGray)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot number badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = TeOrange.copy(alpha = 0.2f)
            ) {
                Text(
                    text = slotNumber.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelMedium,
                    color = TeOrange,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            // File name
            Text(
                text = file.name.substringBeforeLast("."),
                style = MaterialTheme.typography.bodyMedium,
                color = TeLightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Info icon
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Info",
                tint = TeMediumGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PatchFileRow(
    file: LocalFileEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TeDarkGray)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.AudioFile,
                contentDescription = null,
                tint = TeGreen,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                text = file.name.substringBeforeLast("."),
                style = MaterialTheme.typography.bodyMedium,
                color = TeLightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Info icon
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Info",
                tint = TeMediumGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * List view of files within a category.
 */
@Composable
private fun CategoryFilesView(
    categoryName: String,
    folders: List<ProjectFolder>,
    isSlots: Boolean,
    currentTrackName: String?,
    onFileClick: ((LocalFileEntity) -> Unit)? = null,
    onPlayFile: (LocalFileEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allFiles = remember(folders) {
        folders.flatMap { it.files }
            .filter { it.name.lowercase().let { n -> n.endsWith(".aif") || n.endsWith(".wav") } }
            .sortedBy { it.name }
    }
    
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (isSlots) {
            // Special slots view: group by slot number
            val slotFiles = allFiles.groupBy { file ->
                // Extract slot number from path, e.g., /slots/1/file.aif -> 1
                val parts = file.sourcePath.split("/")
                val slotsIndex = parts.indexOfFirst { it.equals("slots", true) }
                if (slotsIndex >= 0 && slotsIndex + 1 < parts.size) {
                    parts[slotsIndex + 1].toIntOrNull() ?: 0
                } else 0
            }.toSortedMap()
            
            slotFiles.forEach { (slotNum, files) ->
                files.forEach { file ->
                    item {
                        SlotFileRow(
                            slotNumber = slotNum,
                            file = file,
                            isPlaying = file.name == currentTrackName,
                            onClick = { onFileClick?.invoke(file) ?: onPlayFile(file) },
                            onPlayClick = { onPlayFile(file) }
                        )
                    }
                }
            }
        } else {
            // Regular file list
            items(allFiles) { file ->
                AudioFileRow(
                    file = file,
                    isPlaying = file.name == currentTrackName,
                    onClick = { onFileClick?.invoke(file) ?: onPlayFile(file) },
                    onPlayClick = { onPlayFile(file) }
                )
            }
        }
        
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun SlotFileRow(
    slotNumber: Int,
    file: LocalFileEntity,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) TeOrange.copy(alpha = 0.1f) else TeDarkGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot number badge
            Surface(
                color = if (isPlaying) TeOrange else TeMediumGray,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = slotNumber.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TeWhite,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = null,
                tint = TeMediumGray,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                text = file.name.removeSuffix(".aif").removeSuffix(".wav"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPlaying) TeOrange else TeLightGray,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Play button
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Outlined.GraphicEq else Icons.Outlined.PlayArrow,
                    contentDescription = "Odtwórz",
                    tint = if (isPlaying) TeOrange else TeGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioFileRow(
    file: LocalFileEntity,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) TeOrange.copy(alpha = 0.1f) else TeDarkGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon (will be enhanced when we load metadata)
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.GraphicEq else Icons.Outlined.AudioFile,
                contentDescription = null,
                tint = if (isPlaying) TeOrange else TeGreen,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            Text(
                text = file.name.removeSuffix(".aif").removeSuffix(".wav"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPlaying) TeOrange else TeLightGray,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = formatSizeCompact(file.size),
                style = MaterialTheme.typography.bodySmall,
                color = TeMediumGray
            )
            
            Spacer(Modifier.width(8.dp))
            
            // Play button
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = "Odtwórz",
                    tint = if (isPlaying) TeOrange else TeGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Groups ProjectFolders by their category (subcategory folder name).
 * For example, files in /drum/user/ are grouped under "user".
 */
private fun groupFoldersByCategory(folders: List<ProjectFolder>): Map<String, List<ProjectFolder>> {
    return folders.groupBy { folder ->
        // The folderName in our data model represents the immediate parent folder
        // For drum/synth, this is the category like "user", "slots", "chords"
        folder.folderName.lowercase()
    }.filterKeys { it.isNotBlank() }
}

private fun formatSizeCompact(bytes: Long): String = when {
    bytes < 1024 * 1024 -> "${bytes / 1024}K"
    else -> String.format("%.1fM", bytes / (1024.0 * 1024.0))
}

/**
 * Mixdown screen - simple list of exported mixes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixdownScreen(
    onNavigateBack: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.audioPlayerManager.playbackState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.setFilter(FileTypeFilter.MIXDOWN)
    }
    
    LaunchedEffect(playbackState.isPlaying) {
        while (playbackState.isPlaying) {
            delay(100)
            viewModel.audioPlayerManager.updatePosition()
        }
    }
    
    val allMixdownFiles = remember(uiState.filteredFolders) {
        uiState.filteredFolders.flatMap { it.files }
            .filter { it.name.lowercase().let { n -> n.endsWith(".aif") || n.endsWith(".wav") } }
            .sortedByDescending { it.downloadedAt }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.MusicNote, null, tint = TeOrange, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("MIKSY", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TeBlack, titleContentColor = TeLightGray)
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
        
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) {
                CircularProgressIndicator(color = TeOrange)
            }
        } else if (allMixdownFiles.isEmpty()) {
            EmptyLibraryView(Icons.Outlined.MusicNote, "Brak miksów\nPobierz z OP-1", Modifier.fillMaxSize().padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Text(
                        "${allMixdownFiles.size} miksów",
                        style = MaterialTheme.typography.bodySmall,
                        color = TeMediumGray
                    )
                }
                
                items(allMixdownFiles) { file ->
                    val isPlaying = file.name == playbackState.currentTrack?.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playFile(file) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlaying) TeOrange.copy(alpha = 0.1f) else TeDarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isPlaying) Icons.Outlined.GraphicEq else Icons.Outlined.MusicNote,
                                null,
                                tint = if (isPlaying) TeOrange else TeGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.name.removeSuffix(".aif").removeSuffix(".wav"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isPlaying) TeOrange else TeLightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    formatSizeCompact(file.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TeMediumGray
                                )
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}
