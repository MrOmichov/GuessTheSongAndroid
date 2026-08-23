package org.mromichov.guessthesong.presentation.game

import org.mromichov.guessthesong.domain.model.Track

sealed class GameUiState {
    data class ListeningPreview(val currentRoundNumber: Int) : GameUiState()
    data class Guessing(val currentRoundNumber: Int, val options: List<Track>) : GameUiState()
    data class Answer(val currentRoundNumber: Int, val answer: Track, val isGuessed: Boolean) : GameUiState()
    data class GameOver(val score: Int) : GameUiState()
}