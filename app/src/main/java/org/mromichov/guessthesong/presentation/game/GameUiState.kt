package org.mromichov.guessthesong.presentation.game

import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track

sealed class GameUiState {
    data class Idle(val playlist: Playlist) : GameUiState()
    data class Guessing(val currentTrack: Track) : GameUiState()
    data class ListeningPreview(val currentTrack: Track) : GameUiState()
    data class GameOver(val score: Int) : GameUiState()
}