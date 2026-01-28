package com.op1sync.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.op1sync.app.core.audio.AudioPlayerManager
import com.op1sync.app.data.local.FileType
import com.op1sync.app.data.local.LocalFileEntity
import com.op1sync.app.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val files: List<LocalFileEntity> = emptyList(),
    val filteredFiles: List<LocalFileEntity> = emptyList(),
    val selectedFilter: FileTypeFilter = FileTypeFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null
)

enum class FileTypeFilter {
    ALL, TAPES, SYNTH, DRUM, MIXDOWN, FAVORITES
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    val audioPlayerManager: AudioPlayerManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadFiles()
    }
    
    private fun loadFiles() {
        viewModelScope.launch {
            libraryRepository.getAllFiles()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { files ->
                    _uiState.update { state ->
                        state.copy(
                            files = files,
                            filteredFiles = applyFilter(files, state.selectedFilter, state.searchQuery),
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    fun setFilter(filter: FileTypeFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredFiles = applyFilter(state.files, filter, state.searchQuery)
            )
        }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredFiles = applyFilter(state.files, state.selectedFilter, query)
            )
        }
    }
    
    private fun applyFilter(
        files: List<LocalFileEntity>,
        filter: FileTypeFilter,
        query: String
    ): List<LocalFileEntity> {
        var result = when (filter) {
            FileTypeFilter.ALL -> files
            FileTypeFilter.TAPES -> files.filter { it.type == FileType.TAPE }
            FileTypeFilter.SYNTH -> files.filter { it.type == FileType.SYNTH }
            FileTypeFilter.DRUM -> files.filter { it.type == FileType.DRUM }
            FileTypeFilter.MIXDOWN -> files.filter { it.type == FileType.MIXDOWN }
            FileTypeFilter.FAVORITES -> files.filter { it.isFavorite }
        }
        
        if (query.isNotBlank()) {
            result = result.filter { it.name.contains(query, ignoreCase = true) }
        }
        
        return result
    }
    
    fun playFile(file: LocalFileEntity) {
        audioPlayerManager.playFromMtp(file.path, file.name)
    }
    
    fun toggleFavorite(file: LocalFileEntity) {
        viewModelScope.launch {
            libraryRepository.toggleFavorite(file)
        }
    }
    
    fun deleteFile(file: LocalFileEntity) {
        viewModelScope.launch {
            val success = libraryRepository.deleteFile(file)
            if (success) {
                _uiState.update { it.copy(successMessage = "Usunięto: ${file.name}") }
            } else {
                _uiState.update { it.copy(error = "Nie udało się usunąć pliku") }
            }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
    
    fun togglePlayPause() {
        audioPlayerManager.togglePlayPause()
    }
    
    fun seekTo(percent: Float) {
        audioPlayerManager.seekToPercent(percent)
    }
    
    fun stopPlayback() {
        audioPlayerManager.stop()
    }
}
