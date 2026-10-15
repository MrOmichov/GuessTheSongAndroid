package org.mromichov.guessthesong.presentation.startinput

import org.mromichov.guessthesong.domain.model.PlaylistContent

sealed class StartInputUiState {
    data class ReadyToPlay(
        val playlist: PlaylistContent
    ) : StartInputUiState()

    data class Preparing(
        val playlist: PlaylistContent
    ) : StartInputUiState()

    data class Loaded(
        val playlist: PlaylistContent
    ) : StartInputUiState()

    object Idle : StartInputUiState()
    object Loading : StartInputUiState()
    data class Error(val message: String) : StartInputUiState()

    data class PlaylistAlreadySavedConflict(
        val savedPlaylistId: Long,
        val savedPlaylistTitle: String,
        val savedPlaylist: PlaylistContent,
    ) : StartInputUiState()
}