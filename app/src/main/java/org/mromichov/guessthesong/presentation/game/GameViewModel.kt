package org.mromichov.guessthesong.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.player.AudioPlayer
import org.mromichov.guessthesong.presentation.game.GameUiState
import javax.inject.Inject
import kotlin.random.Random

class GameViewModel @Inject constructor(
    private val gameManager: GameManager,
    val audioPlayer: AudioPlayer
) : ViewModel() {
    val currentRoundNumber: Int
        get() = gameManager.currentRoundNumber

    private var score = 0
    private val _uiState: MutableStateFlow<GameUiState> = MutableStateFlow(GameUiState.ListeningPreview(currentRoundNumber))
    val uiState = _uiState.asStateFlow()

    init {
        gameManager.nextRound()
        startRoundOrGameOver()
    }

    fun startRoundOrGameOver() {
        viewModelScope.launch {
            if (currentRoundNumber == 5) {
                _uiState.value = GameUiState.GameOver(score)
                gameManager.reset()
            } else {
                if (currentRoundNumber != 1) gameManager.nextRound()
                withContext(Dispatchers.Main) {
                    val start = Random.nextLong(0L, 25000L)
                    val trackPreview = gameManager.getCurrentTrackPreview()
                    audioPlayer.play(trackPreview.previewUrl, start, start + 5000L)
                }
                    audioPlayer.isPlaying.first { it }
                    _uiState.value = GameUiState.ListeningPreview(currentRoundNumber)
                    audioPlayer.isPlaying.first { !it }

                val options = mutableListOf(gameManager.getCurrentRound().track)
                options.addAll(gameManager.getCurrentRound().wrongOptions)
                options.shuffle()


                _uiState.value = GameUiState.Guessing(
                    currentRoundNumber,
                    options
                )
            }
        }
    }

    fun checkAnswer(track: Track) {
        val rightAnswer = gameManager.rightAnswer()
        val isGuessed = rightAnswer == track
        if (isGuessed) score++
        _uiState.value = GameUiState.Answer(currentRoundNumber, rightAnswer, isGuessed)
    }

}