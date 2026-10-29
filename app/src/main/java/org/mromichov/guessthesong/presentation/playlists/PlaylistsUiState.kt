package org.mromichov.guessthesong.presentation.playlists

import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist

sealed class PlaylistsUiState {
    data class Loaded(val playlists: List<LocalPlaylist>) : PlaylistsUiState()
    object LoadedEmpty : PlaylistsUiState()
    object Loading : PlaylistsUiState()
    data class DeletePlaylistIntent(val id: Long, val playlistsLoaded: List<LocalPlaylist>) : PlaylistsUiState()
    data class Error(val message: String) : PlaylistsUiState()
}
