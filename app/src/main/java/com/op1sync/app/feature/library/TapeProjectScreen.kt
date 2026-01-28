package com.op1sync.app.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.op1sync.app.core.audio.MultiTrackPlayer
import com.op1sync.app.ui.components.*
import com.op1sync.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * Tape project detail screen with tape deck visualization and playback controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapeProjectScreen(
    folderPath: String,
    onNavigateBack: () -> Unit,
    viewModel: TapeProjectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(folderPath) {
        viewModel.loadProject(folderPath)
    }
    
    // Periodic position update while playing
    LaunchedEffect(uiState.isPlaying) {
        while (uiState.isPlaying) {
            kotlinx.coroutines.delay(100)
            viewModel.updatePosition()
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
                        Column {
                            Text(
                                text = uiState.projectName.ifEmpty { "Projekt" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.trackCount > 0) {
                                Text(
                                    text = "${uiState.trackCount} ścieżek",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TeMediumGray
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Wstecz")
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
                            text = uiState.error ?: "Błąd wczytywania projektu",
                            color = TeMediumGray
                        )
                        OutlinedButton(onClick = onNavigateBack) {
                            Text("Wróć")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Tape deck with timer
                        TapeDeckImage(
                            isPlaying = uiState.isPlaying,
                            currentTimeMs = uiState.currentPositionMs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Track strips
                        TrackStrips(
                            trackRegions = uiState.trackRegions,
                            totalSamples = uiState.totalSamples,
                            currentPosition = uiState.currentPositionSamples,
                            trackMuted = uiState.trackMuted,
                            onTrackMuteToggle = { track, muted ->
                                viewModel.toggleTrackMute(track, muted)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Seek bar
                        TapeSeekBar(
                            currentPosition = uiState.currentPositionSamples,
                            totalSamples = uiState.totalSamples,
                            onSeek = { viewModel.seekTo(it) },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(Modifier.weight(1f))
                        
                        // Playback controls
                        TapeControls(
                            isPlaying = uiState.isPlaying,
                            onPlay = { viewModel.play() },
                            onPause = { viewModel.pause() },
                            onStop = { viewModel.stop() },
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

data class TapeProjectUiState(
    val isLoading: Boolean = true,
    val projectName: String = "",
    val folderPath: String = "",
    val trackCount: Int = 0,
    val trackRegions: List<List<TrackRegion>> = listOf(emptyList(), emptyList(), emptyList(), emptyList()),
    val trackMuted: List<Boolean> = listOf(false, false, false, false),
    val trackFiles: List<String> = emptyList(),  // Paths to track.aif files
    val totalSamples: Long = 0,
    val currentPositionSamples: Long = 0,
    val currentPositionMs: Long = 0,
    val isPlaying: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TapeProjectViewModel @Inject constructor(
    private val multiTrackPlayer: MultiTrackPlayer
) : ViewModel() {
    
    companion object {
        private const val SAMPLE_RATE = 44100L
    }
    
    private val _uiState = MutableStateFlow(TapeProjectUiState())
    val uiState: StateFlow<TapeProjectUiState> = _uiState.asStateFlow()
    
    init {
        // Observe playback state from MultiTrackPlayer
        viewModelScope.launch {
            multiTrackPlayer.playbackState.collect { playbackState ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = playbackState.isPlaying,
                    currentPositionMs = playbackState.positionMs,
                    currentPositionSamples = playbackState.positionSamples,
                    trackMuted = playbackState.trackMuted
                )
            }
        }
    }
    
    fun loadProject(folderPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val folder = File(folderPath)
                if (!folder.exists() || !folder.isDirectory) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Folder nie istnieje"
                    )
                    return@launch
                }
                
                val tapeJsonFile = File(folder, "tape.json")
                if (!tapeJsonFile.exists()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Brak pliku tape.json"
                    )
                    return@launch
                }
                
                val json = JSONObject(tapeJsonFile.readText())
                val clips = json.getJSONArray("clips")
                
                // Parse clips into track regions (4 tracks: 0-3)
                val trackRegions = List(4) { mutableListOf<TrackRegion>() }
                var maxSample = 0L
                
                for (i in 0 until clips.length()) {
                    val clip = clips.getJSONObject(i)
                    val channel = clip.getInt("ch")
                    val start = clip.getLong("start")
                    val stop = clip.getLong("stop")
                    
                    if (channel in 0..3) {
                        trackRegions[channel].add(TrackRegion(start, stop))
                        if (stop > maxSample) maxSample = stop
                    }
                }
                
                // Sort regions by start time
                trackRegions.forEach { it.sortBy { region -> region.startSample } }
                
                // Find track audio files
                val trackFiles = (1..4).map { trackNum ->
                    val trackFile = File(folder, "track_$trackNum.wav")
                    if (trackFile.exists()) trackFile.absolutePath else ""
                }
                
                // Prepare multi-track player
                multiTrackPlayer.prepare(trackFiles)
                
                // Ensure minimum tape length (1 minute)
                val minSamples = SAMPLE_RATE * 60
                if (maxSample < minSamples) maxSample = minSamples
                
                // Count tracks with content
                val tracksWithContent = trackRegions.count { it.isNotEmpty() }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    projectName = folder.name,
                    folderPath = folderPath,
                    trackCount = tracksWithContent,
                    trackRegions = trackRegions,
                    trackFiles = trackFiles,
                    totalSamples = maxSample
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Błąd: ${e.message}"
                )
            }
        }
    }
    
    fun play() {
        multiTrackPlayer.play()
    }
    
    fun pause() {
        multiTrackPlayer.pause()
    }
    
    fun stop() {
        multiTrackPlayer.stop()
    }
    
    fun seekTo(positionSamples: Long) {
        multiTrackPlayer.seekToSamples(positionSamples)
    }
    
    fun toggleTrackMute(trackIndex: Int, muted: Boolean) {
        multiTrackPlayer.toggleTrackMute(trackIndex, muted)
    }
    
    fun updatePosition() {
        // Position is updated via playbackState flow
    }
    
    override fun onCleared() {
        super.onCleared()
        multiTrackPlayer.release()
    }
}


