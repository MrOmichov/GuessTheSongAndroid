package org.mromichov.guessthesong.presentation.playlists

import org.mromichov.guessthesong.domain.model.Playlist

sealed class PlaylistsUiState {
    data class Loaded(val playlists: List<Playlist>) : PlaylistsUiState()
    object LoadedEmpty : PlaylistsUiState()
    object Loading : PlaylistsUiState()
    data class Error(val message: String) : PlaylistsUiState()
}