package org.mromichov.guessthesong.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.player.AudioPlayer
import javax.inject.Inject
import kotlin.random.Random

class GameViewModel @Inject constructor(
    private val gameManager: GameManager,
    val audioPlayer: AudioPlayer,
) : ViewModel() {
    val currentRoundIndex: Int
        get() = gameManager.currentRoundIndex

    val score: Int
        get() = gameManager.score

    private val _uiState: MutableStateFlow<GameUiState> =
        MutableStateFlow(GameUiState.ListeningPreview(currentRoundIndex))
    val uiState = _uiState.asStateFlow()
    private var isListeningStarted = false

    init {
        startRoundOrGameOver()
    }

    fun startRoundOrGameOver() {
        if (!isListeningStarted) {
            viewModelScope.launch {
                if (currentRoundIndex == gameManager.config.roundsNumber) {
                    _uiState.value = GameUiState.GameOver(score)
                } else {
                    val start = Random.nextLong(0L, 30000L - gameManager.config.snippetLength)
                    val trackPreview = gameManager.getCurrentTrackPreview()
                    isListeningStarted = true
                    audioPlayer.play(trackPreview.trackPreviewUrl, start, start + gameManager.config.snippetLength)
                    _uiState.value = GameUiState.ListeningPreview(currentRoundIndex)
                    val combinedAudioPlayerState =
                        combine(audioPlayer.snippetFinished, audioPlayer.error) { snippetFinished, error ->
                            snippetFinished to error
                        }
                    val result = combinedAudioPlayerState.first { (snippetFinished, error) ->
                        error != null || snippetFinished
                    }
                    if (result.second != null) {
                        _uiState.value = GameUiState.Error(result.second!!.message ?: "Unknown error")
                        return@launch
                    }
                    isListeningStarted = false
                    _uiState.value = GameUiState.Guessing(
                        currentRoundIndex,
                        gameManager.currentRoundOptions
                    )
                }
            }
        }
    }

    fun checkAnswer(userAnswer: Track) {
        if (_uiState.value !is GameUiState.Guessing) return
        val isGuessed = gameManager.checkAnswer(userAnswer)
        _uiState.value = GameUiState.Answer(currentRoundIndex, gameManager.getCurrentRound().track, isGuessed)
        gameManager.nextRound()
    }
}
