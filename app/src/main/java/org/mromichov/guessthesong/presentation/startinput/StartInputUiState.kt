package org.mromichov.guessthesong.presentation.startinput

import org.mromichov.guessthesong.domain.model.Playlist

// TODO Добавить state Preparing, переименовать Success в ReadyToPlay
sealed class StartInputUiState {
    data class ReadyToPlay(
        val playlist: Playlist
    ) : StartInputUiState()

    data class Preparing(
        val playlist: Playlist
    ) : StartInputUiState()

    data class Loaded(
        val playlist: Playlist
    ) : StartInputUiState()

    object Idle : StartInputUiState()
    object Loading : StartInputUiState()
    data class Error(val message: String) : StartInputUiState()
}