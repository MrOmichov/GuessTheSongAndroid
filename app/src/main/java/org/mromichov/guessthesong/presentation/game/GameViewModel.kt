package org.mromichov.guessthesong.presentation.game

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mromichov.guessthesong.domain.game.GameManager
import javax.inject.Inject

class GameViewModel @Inject constructor(
    private val gameManager: GameManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Idle(gameManager.currentPlaylist!!))
    val uiState = _uiState.asStateFlow()
}