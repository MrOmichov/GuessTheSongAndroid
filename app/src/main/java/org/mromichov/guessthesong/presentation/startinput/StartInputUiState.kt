package org.mromichov.guessthesong.presentation.startinput

import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.RemotePlaylist

sealed class StartInputUiState {
    object Menu : StartInputUiState()
    object ImportForm : StartInputUiState()
    object Importing : StartInputUiState()

    data class PlaylistAlreadySavedConflict(
        val alreadyExistingPlaylist: LocalPlaylist,
        val fetchedPlaylist: RemotePlaylist,
    ) : StartInputUiState()

    data class ImportError(val message: String) : StartInputUiState()

    data class SelectingSavedPlaylist(val id: Long) : StartInputUiState()
    data class SelectionError(val message: String) : StartInputUiState()

    data class Selected(val playlist: PlaylistContent) : StartInputUiState()
    data class Preparing(val playlist: PlaylistContent) : StartInputUiState()
    data class CancellingPreparation(val playlist: PlaylistContent) : StartInputUiState()
    data class PreparationError(
        val message: String,
        val playlist: PlaylistContent,
    ) : StartInputUiState()

    data class ReadyToPlay(val playlist: PlaylistContent) : StartInputUiState()

    /** The cancelled repository operation has not finished unwinding yet. */
    object CancellingToMenu : StartInputUiState()
}
