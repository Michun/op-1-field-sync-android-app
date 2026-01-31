package com.op1sync.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.op1sync.app.core.audio.DrumSampleData
import com.op1sync.app.core.audio.DrumSamplePlayer
import com.op1sync.app.core.audio.OP1PatchParser
import com.op1sync.app.ui.components.DrumSampleNames
import com.op1sync.app.ui.components.OP1Keyboard
import com.op1sync.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumKeyboardScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    viewModel: DrumKeyboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(filePath) {
        viewModel.loadDrumKit(filePath)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.release()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (uiState.isDBox) Icons.Outlined.MusicNote else Icons.Outlined.Sensors,
                            null,
                            tint = if (uiState.isDBox) TeBlue else TeOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = uiState.drumName.ifEmpty { if (uiState.isDBox) "D-BOX" else "DRUM KIT" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isDBox) "Synthesizer" else "${uiState.sampleCount} samples",
                                style = MaterialTheme.typography.bodySmall,
                                color = TeMediumGray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = TeOrange)
                }
                uiState.error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = uiState.error ?: "Error loading drum kit",
                            color = TeMediumGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        OutlinedButton(onClick = onNavigateBack) {
                            Text("Go back")
                        }
                    }
                }
                uiState.isDBox -> {
                    // D-Box synthesizer - cannot play samples
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MusicNote,
                            null,
                            tint = TeBlue,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "D-Box Synthesizer",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TeLightGray
                        )
                        Text(
                            text = "D-Box to syntezator generujący dźwięki algorytmicznie.\n\nOdtwarzanie nie jest możliwe - dźwięki są tworzone w czasie rzeczywistym przez OP-1.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TeMediumGray,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(onClick = onNavigateBack) {
                            Text("Wróć")
                        }
                    }
                }
                else -> {
                    // Sample-based drum kit - show keyboard
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Patch metadata
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.drumName.ifEmpty { "Drum Kit" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TeLightGray
                            )
                            Text(
                                text = "${uiState.drumType} • ${uiState.sampleCount} samples",
                                style = MaterialTheme.typography.bodySmall,
                                color = TeMediumGray
                            )
                        }
                        
                        Spacer(Modifier.weight(1f))
                        
                        // Keyboard
                        OP1Keyboard(
                            sampleNames = DrumSampleNames.defaults,
                            onKeyPress = { index ->
                                viewModel.playSample(index)
                            }
                        )
                        
                        Spacer(Modifier.weight(1f))
                        
                        // Hint
                        Text(
                            text = "Tap keys to play samples",
                            color = TeMediumGray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                }
            }
        }
    }
}


data class DrumKeyboardUiState(
    val isLoading: Boolean = true,
    val drumName: String = "",
    val drumType: String = "",
    val sampleCount: Int = 0,
    val isDBox: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DrumKeyboardViewModel @Inject constructor(
    private val patchParser: OP1PatchParser,
    private val samplePlayer: DrumSamplePlayer
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DrumKeyboardUiState())
    val uiState: StateFlow<DrumKeyboardUiState> = _uiState.asStateFlow()
    
    fun loadDrumKit(filePath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val file = File(filePath)
            if (!file.exists()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "File not found"
                )
                return@launch
            }
            
            val drumData: DrumSampleData? = patchParser.parseDrumSamples(file)
            
            if (drumData != null) {
                // Check if it's a D-Box synthesizer patch
                val isDBox = drumData.type.equals("dbox", ignoreCase = true)
                
                if (isDBox) {
                    // D-Box patches cannot be played - they're synthesizer-based
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        drumName = drumData.name,
                        drumType = drumData.type,
                        isDBox = true,
                        sampleCount = 0
                    )
                } else {
                    // Sample-based drum kit - load samples
                    samplePlayer.loadSamples(
                        drumData.ssndData,
                        drumData.startPositions,
                        drumData.endPositions
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        drumName = drumData.name,
                        drumType = drumData.type,
                        isDBox = false,
                        sampleCount = samplePlayer.getSampleCount()
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Could not parse drum kit"
                )
            }
        }
    }
    
    fun playSample(index: Int) {
        samplePlayer.play(index)
    }
    
    fun release() {
        // Don't release the singleton player
    }
    
    override fun onCleared() {
        super.onCleared()
        // Singleton player persists
    }
}

