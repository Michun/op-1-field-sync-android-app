package com.op1sync.app.feature.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.op1sync.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "BACKUP",
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
        containerColor = TeBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Google Drive Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TeDarkGray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = null,
                        tint = TeMediumGray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Drive",
                            style = MaterialTheme.typography.titleMedium,
                            color = TeLightGray
                        )
                        Text(
                            text = "Nie zalogowano",
                            style = MaterialTheme.typography.bodySmall,
                            color = TeMediumGray
                        )
                    }
                    Button(
                        onClick = { /* TODO: Google Sign In */ },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TeOrange)
                    ) {
                        Text("ZALOGUJ")
                    }
                }
            }
            
            // Backup Action Cards
            Text(
                text = "── AKCJE ──",
                style = MaterialTheme.typography.labelMedium,
                color = TeMediumGray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            BackupActionCard(
                icon = Icons.Outlined.CloudUpload,
                title = "Pełny Backup",
                description = "Zapisz wszystkie pliki z OP-1 do chmury",
                onClick = { /* TODO */ },
                enabled = false
            )
            
            BackupActionCard(
                icon = Icons.Outlined.CloudDownload,
                title = "Przywróć z Backupu",
                description = "Odtwórz pliki z chmury na OP-1",
                onClick = { /* TODO */ },
                enabled = false
            )
            
            BackupActionCard(
                icon = Icons.Outlined.History,
                title = "Historia Backupów",
                description = "Przeglądaj poprzednie kopie zapasowe",
                onClick = { /* TODO */ },
                enabled = false
            )
        }
    }
}

@Composable
private fun BackupActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) TeDarkGray else TeDarkGray.copy(alpha = 0.5f)
        ),
        onClick = onClick,
        enabled = enabled
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
                tint = if (enabled) TeOrange else TeMediumGray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) TeLightGray else TeMediumGray
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TeMediumGray
                )
            }
        }
    }
}
