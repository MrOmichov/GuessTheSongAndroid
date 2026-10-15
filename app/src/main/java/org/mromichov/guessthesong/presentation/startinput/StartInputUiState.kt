package org.mromichov.guessthesong.presentation.startinput

import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.RemotePlaylist

sealed class StartInputUiState {
    data class ReadyToPlay(
        val playlist: PlaylistContent
    ) : StartInputUiState()

    data class Preparing(
        val playlist: PlaylistContent
    ) : StartInputUiState()

    data class Loaded(
        val playlistContent: PlaylistContent
    ) : StartInputUiState()

    object Idle : StartInputUiState()
    object Loading : StartInputUiState()
    data class Error(val message: String) : StartInputUiState()

    data class PlaylistAlreadySavedConflict(
        val alreadyExistingPlaylist: LocalPlaylist,
        val fetchedPlaylist: RemotePlaylist,
    ) : StartInputUiState()
}