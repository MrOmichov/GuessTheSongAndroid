package org.mromichov.guessthesong.presentation.playlists

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loaded(emptyList()))
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()
    var showDeleteErrorSnackbar by mutableStateOf(false)
        private set

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

    fun showDeleteDialog(id: Long, playlists: List<LocalPlaylist>) {
        if (uiState.value !is PlaylistsUiState.Loaded) return
        _uiState.value = PlaylistsUiState.DeletePlaylistIntent(id, playlists)
    }

    fun deletePlaylist(id: Long) {
        if (uiState.value !is PlaylistsUiState.DeletePlaylistIntent) return
        viewModelScope.launch {
            showDeleteErrorSnackbar = false
            playlistRepository.deletePlaylist(id)
                .onSuccess {
                    loadPlaylists()
                }
                .onFailure {
                    showDeleteErrorSnackbar = true
                }
        }
    }

    fun backToLoaded(playlists: List<LocalPlaylist>) {
        if (uiState.value !is PlaylistsUiState.DeletePlaylistIntent) return
        _uiState.value = PlaylistsUiState.Loaded(playlists)
    }
}