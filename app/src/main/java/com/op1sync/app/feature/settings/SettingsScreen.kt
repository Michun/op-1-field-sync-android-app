package com.op1sync.app.feature.settings

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
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "USTAWIENIA",
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sync Settings
            Text(
                text = "── SYNCHRONIZACJA ──",
                style = MaterialTheme.typography.labelMedium,
                color = TeMediumGray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            var autoSync by remember { mutableStateOf(false) }
            
            SettingsToggleItem(
                title = "Auto-sync",
                description = "Automatycznie synchronizuj po połączeniu",
                checked = autoSync,
                onCheckedChange = { autoSync = it }
            )
            
            var wifiOnly by remember { mutableStateOf(true) }
            
            SettingsToggleItem(
                title = "Tylko WiFi",
                description = "Używaj tylko połączenia WiFi do backupu",
                checked = wifiOnly,
                onCheckedChange = { wifiOnly = it }
            )
            
            // Storage Settings
            Text(
                text = "── PAMIĘĆ ──",
                style = MaterialTheme.typography.labelMedium,
                color = TeMediumGray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            SettingsClickItem(
                title = "Lokalizacja backupów",
                description = "/storage/emulated/0/OP1Sync",
                onClick = { /* TODO: Open folder picker */ }
            )
            
            SettingsClickItem(
                title = "Wyczyść cache",
                description = "0 MB używane",
                onClick = { /* TODO: Clear cache */ }
            )
            
            // About
            Text(
                text = "── INFORMACJE ──",
                style = MaterialTheme.typography.labelMedium,
                color = TeMediumGray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            SettingsClickItem(
                title = "Wersja aplikacji",
                description = "0.1.0 (MVP)",
                onClick = { }
            )
            
            SettingsClickItem(
                title = "GitHub",
                description = "Kod źródłowy",
                onClick = { /* TODO: Open GitHub */ }
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TeDarkGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeLightGray
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TeMediumGray
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TeWhite,
                    checkedTrackColor = TeOrange,
                    uncheckedThumbColor = TeMediumGray,
                    uncheckedTrackColor = TeBlack
                )
            )
        }
    }
}

@Composable
private fun SettingsClickItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TeDarkGray),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TeLightGray
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TeMediumGray
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TeMediumGray
            )
        }
    }
}
