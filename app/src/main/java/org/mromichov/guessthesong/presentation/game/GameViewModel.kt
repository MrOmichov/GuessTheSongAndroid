package org.mromichov.guessthesong.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.player.AudioPlayer
import javax.inject.Inject
import kotlin.random.Random

class GameViewModel @Inject constructor(
    private val gameManager: GameManager,
    val audioPlayer: AudioPlayer
) : ViewModel() {
    var currentRoundNumber = gameManager.currentRoundNumber
        private set
        get() = field + 1

    private var score = 0
    private val _uiState: MutableStateFlow<GameUiState> = MutableStateFlow(GameUiState.ListeningPreview(currentRoundNumber))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            startRoundOrGameOver()
            val options = mutableListOf(gameManager.getCurrentRound().track)
            options.addAll(gameManager.getCurrentRound().wrongOptions)
            options.shuffle()
            audioPlayer.isPlaying.first { it }
            audioPlayer.isPlaying.first { !it }
            _uiState.value = GameUiState.Guessing(
                gameManager.currentRoundNumber,
                options
            )
        }
    }

    fun startRoundOrGameOver() {
        if (gameManager.currentRoundNumber == 5) {
            _uiState.value = GameUiState.GameOver(score)
            gameManager.reset()
        }
        gameManager.nextRound()
        val trackPreview = gameManager.getCurrentTrackPreview()
        val start = Random.nextLong(0L, 25000L)
        audioPlayer.play(trackPreview.previewUrl, start, start + 5000L)
    }

    fun checkAnswer(track: Track) {
        val rightAnswer = gameManager.rightAnswer()
        val isGuessed = rightAnswer == track
        if (isGuessed) score++
        _uiState.value = GameUiState.Answer(currentRoundNumber, rightAnswer, isGuessed)
    }
}