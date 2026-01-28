package com.op1sync.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.op1sync.app.core.audio.AudioPlayerManager
import com.op1sync.app.data.local.FileType
import com.op1sync.app.data.local.LocalFileEntity
import com.op1sync.app.data.local.ProjectFolder
import com.op1sync.app.data.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val folders: List<ProjectFolder> = emptyList(),
    val filteredFolders: List<ProjectFolder> = emptyList(),
    val selectedFilter: FileTypeFilter = FileTypeFilter.ALL,
    val searchQuery: String = "",
    val expandedFolder: String? = null, // Currently expanded folder name
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
        loadFolders()
    }
    
    private fun loadFolders() {
        viewModelScope.launch {
            libraryRepository.getProjectFolders()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { folders ->
                    _uiState.update { state ->
                        state.copy(
                            folders = folders,
                            filteredFolders = applyFilter(folders, state.selectedFilter, state.searchQuery),
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
                filteredFolders = applyFilter(state.folders, filter, state.searchQuery),
                expandedFolder = null // Collapse when changing filter
            )
        }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredFolders = applyFilter(state.folders, state.selectedFilter, query)
            )
        }
    }
    
    private fun applyFilter(
        folders: List<ProjectFolder>,
        filter: FileTypeFilter,
        query: String
    ): List<ProjectFolder> {
        var result = when (filter) {
            FileTypeFilter.ALL -> folders
            FileTypeFilter.TAPES -> folders.filter { it.type == FileType.TAPE }
            FileTypeFilter.SYNTH -> folders.filter { it.type == FileType.SYNTH }
            FileTypeFilter.DRUM -> folders.filter { it.type == FileType.DRUM }
            FileTypeFilter.MIXDOWN -> folders.filter { it.type == FileType.MIXDOWN }
            FileTypeFilter.FAVORITES -> folders.filter { it.isFavorite }
        }
        
        if (query.isNotBlank()) {
            result = result.filter { folder ->
                folder.folderName.contains(query, ignoreCase = true) ||
                folder.files.any { it.name.contains(query, ignoreCase = true) }
            }
        }
        
        return result
    }
    
    fun toggleFolderExpanded(folder: ProjectFolder) {
        _uiState.update { state ->
            state.copy(
                expandedFolder = if (state.expandedFolder == folder.folderName) null 
                    else folder.folderName
            )
        }
    }
    
    fun playFile(file: LocalFileEntity) {
        audioPlayerManager.playFromMtp(file.path, file.name)
    }
    
    fun playFirstInFolder(folder: ProjectFolder) {
        // Find first audio file in folder
        val audioFile = folder.files.firstOrNull { file ->
            val lower = file.name.lowercase()
            lower.endsWith(".wav") || lower.endsWith(".aif") || lower.endsWith(".aiff")
        }
        audioFile?.let { playFile(it) }
    }
    
    fun toggleFolderFavorite(folder: ProjectFolder) {
        viewModelScope.launch {
            libraryRepository.toggleFolderFavorite(folder)
        }
    }
    
    fun deleteFolder(folder: ProjectFolder) {
        viewModelScope.launch {
            val success = libraryRepository.deleteFolder(folder)
            if (success) {
                _uiState.update { it.copy(successMessage = "Usunięto: ${folder.folderName}") }
            } else {
                _uiState.update { it.copy(error = "Nie udało się usunąć folderu") }
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
