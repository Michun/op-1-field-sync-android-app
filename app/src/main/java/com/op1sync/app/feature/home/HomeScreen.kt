package com.op1sync.app.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.op1sync.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToBrowser: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "OP-1 SYNC",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Ustawienia"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TeBlack,
                    titleContentColor = TeLightGray
                )
            )
        },
        containerColor = TeBlack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            item {
                ConnectionStatusCard(
                    isConnected = uiState.isConnected,
                    deviceName = uiState.deviceName,
                    onConnectClick = { viewModel.toggleConnection() }
                )
            }
            
            // Quick Actions
            item {
                Text(
                    text = "── SZYBKIE AKCJE ──",
                    style = MaterialTheme.typography.labelMedium,
                    color = TeMediumGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Outlined.FolderOpen,
                        title = "PRZEGLĄDAJ",
                        subtitle = "Pliki OP-1",
                        onClick = onNavigateToBrowser,
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isConnected
                    )
                    QuickActionCard(
                        icon = Icons.Outlined.CloudUpload,
                        title = "BACKUP",
                        subtitle = "Do chmury",
                        onClick = onNavigateToBackup,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Categories
            item {
                Text(
                    text = "── BIBLIOTEKA ──",
                    style = MaterialTheme.typography.labelMedium,
                    color = TeMediumGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard(
                        icon = Icons.Outlined.Album,
                        title = "TAŚMY",
                        count = uiState.tapesCount,
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.weight(1f)
                    )
                    CategoryCard(
                        icon = Icons.Outlined.Piano,
                        title = "SYNTH",
                        count = uiState.synthCount,
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard(
                        icon = Icons.Outlined.Sensors,
                        title = "DRUM",
                        count = uiState.drumCount,
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.weight(1f)
                    )
                    CategoryCard(
                        icon = Icons.Outlined.MusicNote,
                        title = "MIKSY",
                        count = uiState.mixdownCount,
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Recent Items
            if (uiState.recentItems.isNotEmpty()) {
                item {
                    Text(
                        text = "── OSTATNIE ──",
                        style = MaterialTheme.typography.labelMedium,
                        color = TeMediumGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(uiState.recentItems.take(5)) { item ->
                    RecentItemCard(
                        name = item.name,
                        type = item.type,
                        date = item.date,
                        onClick = { /* TODO: Open item */ }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    deviceName: String?,
    onConnectClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isConnected) TeDarkGray else TeDarkGray,
        animationSpec = tween(300),
        label = "bg_color"
    )
    val statusColor by animateColorAsState(
        targetValue = if (isConnected) TeGreen else TeMediumGray,
        animationSpec = tween(300),
        label = "status_color"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName ?: "OP-1 FIELD",
                    style = MaterialTheme.typography.titleMedium,
                    color = TeLightGray
                )
                Text(
                    text = if (isConnected) "Połączono" else "Nie połączono",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
            
            Button(
                onClick = onConnectClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) TeMediumGray else TeOrange
                )
            ) {
                Text(
                    text = if (isConnected) "ROZŁĄCZ" else "POŁĄCZ",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) TeDarkGray else TeDarkGray.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) TeOrange else TeMediumGray,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) TeLightGray else TeMediumGray
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TeMediumGray
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    icon: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TeDarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TeOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TeLightGray
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = TeMediumGray
            )
        }
    }
}

@Composable
private fun RecentItemCard(
    name: String,
    type: String,
    date: String,
    onClick: () -> Unit
) {
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
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = TeOrange,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeLightGray
                )
                Text(
                    text = "$type • $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = TeMediumGray
                )
            }
        }
    }
}
