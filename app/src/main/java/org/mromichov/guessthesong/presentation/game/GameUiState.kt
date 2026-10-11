package org.mromichov.guessthesong.presentation.game

import org.mromichov.guessthesong.domain.model.Track

sealed class GameUiState {
    data class ListeningPreview(val currentRoundIndex: Int) : GameUiState()
    data class Guessing(val currentRoundIndex: Int, val options: List<Track>) : GameUiState()
    data class Answer(val currentRoundIndex: Int, val answer: Track, val isGuessed: Boolean) : GameUiState()
    data class GameOver(val score: Int) : GameUiState()
    data class Error(val message: String) : GameUiState()
}