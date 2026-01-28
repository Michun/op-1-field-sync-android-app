package com.op1sync.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.op1sync.app.core.usb.MtpConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentItem(
    val name: String,
    val type: String,
    val date: String
)

data class HomeUiState(
    val isConnected: Boolean = false,
    val deviceName: String? = null,
    val tapesCount: Int = 0,
    val synthCount: Int = 0,
    val drumCount: Int = 0,
    val mixdownCount: Int = 0,
    val recentItems: List<RecentItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mtpConnectionManager: MtpConnectionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        observeConnectionState()
    }
    
    private fun observeConnectionState() {
        viewModelScope.launch {
            mtpConnectionManager.connectionState.collect { state ->
                _uiState.update { 
                    it.copy(
                        isConnected = state.isConnected,
                        deviceName = state.deviceName
                    )
                }
                
                if (state.isConnected) {
                    loadDeviceStats()
                }
            }
        }
    }
    
    fun toggleConnection() {
        viewModelScope.launch {
            if (_uiState.value.isConnected) {
                mtpConnectionManager.disconnect()
            } else {
                mtpConnectionManager.connect()
            }
        }
    }
    
    private suspend fun loadDeviceStats() {
        _uiState.update { it.copy(isLoading = true) }
        
        try {
            val stats = mtpConnectionManager.getDeviceStats()
            _uiState.update {
                it.copy(
                    tapesCount = stats.tapesCount,
                    synthCount = stats.synthCount,
                    drumCount = stats.drumCount,
                    mixdownCount = stats.mixdownCount,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
}
