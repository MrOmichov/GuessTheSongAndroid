package org.mromichov.guessthesong.presentation.startinput

import org.mromichov.guessthesong.domain.model.Playlist

sealed class StartInputUiState {
    data class Success(
        val playlist: Playlist
    ) : StartInputUiState()
    object Idle : StartInputUiState()
    object Loading : StartInputUiState()
    data class Error(val message: String) : StartInputUiState()
}