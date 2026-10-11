package org.mromichov.guessthesong.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.core.AppConsts
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.player.AudioPlayer
import javax.inject.Inject
import kotlin.random.Random

class GameViewModel @Inject constructor(
    private val gameManager: GameManager,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {
    val currentRoundIndex: Int
        get() = gameManager.currentRoundIndex

    val score: Int
        get() = gameManager.score

    private val _uiState: MutableStateFlow<GameUiState> =
        MutableStateFlow(GameUiState.ListeningPreview(currentRoundIndex))
    val uiState = _uiState.asStateFlow()
    private var isListeningStarted = false
    val currentSeconds: StateFlow<Int> = audioPlayer.currentMs.map {
        (it / 1_000).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), 0)
    val durationSeconds: StateFlow<Int> = audioPlayer.durationMs.map {
        (it / 1_000).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), 0)

    init {
        startRoundOrGameOver()
    }

    fun startRoundOrGameOver() {
        if (!isListeningStarted) {
            viewModelScope.launch {
                if (currentRoundIndex == gameManager.config.roundsNumber) {
                    _uiState.value = GameUiState.GameOver(score)
                } else {
                    val start =
                        if (gameManager.config.snippetLength == AppConsts.MAX_SNIPPET_LENGTH_IN_SECONDS * 1_000L) 0L else Random.nextLong(
                            0L,
                            AppConsts.MAX_SNIPPET_LENGTH_IN_SECONDS * 1_000L - gameManager.config.snippetLength
                        )
                    val trackPreview = gameManager.getCurrentTrackPreview()
                    isListeningStarted = true
                    audioPlayer.play(trackPreview.trackPreviewUrl, start, start + gameManager.config.snippetLength)
                    _uiState.value = GameUiState.ListeningPreview(currentRoundIndex)
                    val combinedAudioPlayerState =
                        combine(audioPlayer.snippetFinished, audioPlayer.error) { snippetFinished, error ->
                            snippetFinished to error
                        }
                    val finishedOrErrorPair = combinedAudioPlayerState.first { (snippetFinished, error) ->
                        error != null || snippetFinished
                    }
                    if (finishedOrErrorPair.second != null) {
                        _uiState.value = GameUiState.Error(finishedOrErrorPair.second!!.message ?: "Unknown error")
                        isListeningStarted = false
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
