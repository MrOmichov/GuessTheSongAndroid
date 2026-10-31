package org.mromichov.guessthesong.presentation.game

import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.presentation.game.artwork.PreparedCover

sealed class GameUiState {
    data class ListeningPreview(val currentRoundIndex: Int, val preparedCover: PreparedCover? = null) : GameUiState()
    data class Guessing(val currentRoundIndex: Int, val options: List<Track>, val preparedCover: PreparedCover? = null) : GameUiState()
    data class Answer(val currentRoundIndex: Int, val answer: Track, val isGuessed: Boolean, val preparedCover: PreparedCover? = null) : GameUiState()
    data class GameOver(val score: Int) : GameUiState()
    data class Error(val message: String) : GameUiState()
}
