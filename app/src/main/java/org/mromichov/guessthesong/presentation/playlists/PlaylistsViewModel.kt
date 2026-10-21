package org.mromichov.guessthesong.presentation.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loaded(emptyList()))
    val uiState: StateFlow<PlaylistsUiState> = _uiState

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        if (uiState.value is PlaylistsUiState.Loading) return
        _uiState.value = PlaylistsUiState.Loading
        viewModelScope.launch {
            playlistRepository.getLocalPlaylists()
                .onSuccess { playlists ->
                    if (playlists.isEmpty())
                        _uiState.value = PlaylistsUiState.LoadedEmpty
                    else
                        _uiState.value = PlaylistsUiState.Loaded(playlists)
                }
                .onFailure { error ->
                    _uiState.value = PlaylistsUiState.Error(error.message ?: "Unknown error")
                }
        }
    }


}