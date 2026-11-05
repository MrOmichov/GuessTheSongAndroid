package org.mromichov.guessthesong.presentation.game

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.core.AppConsts
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.GameRound
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.player.AudioPlayer
import org.mromichov.guessthesong.presentation.error.UserErrorContext
import org.mromichov.guessthesong.presentation.error.UserErrorMapper
import org.mromichov.guessthesong.presentation.game.artwork.PrepareTrackCover
import org.mromichov.guessthesong.presentation.game.artwork.PreparedCover
import javax.inject.Inject
import kotlin.random.Random

class GameViewModel @Inject constructor(
    private val gameManager: GameManager,
    private val audioPlayer: AudioPlayer,
    private val prepareTrackCover: PrepareTrackCover,
) : ViewModel() {
    val currentRoundIndex: Int get() = gameManager.currentRoundIndex
    val score: Int get() = gameManager.score

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.ListeningPreview(currentRoundIndex))
    val uiState = _uiState.asStateFlow()
    private var roundJob: Job? = null
    val currentMs: StateFlow<Long> = audioPlayer.currentMs
    val durationMs: StateFlow<Long> = audioPlayer.durationMs

    init {
        startRoundOrGameOver()
    }

    // TODO UB рефакторинг!!!
    fun startRoundOrGameOver() {
        if (roundJob?.isActive == true || _uiState.value is GameUiState.Guessing) return
        val index = currentRoundIndex
        if (index == gameManager.config.roundsNumber) {
            _uiState.value = GameUiState.GameOver(score)
            return
        }
        val round = gameManager.getCurrentRound()

        _uiState.value = GameUiState.ListeningPreview(index)
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val cover = prepareTrackCover.prepare(round.trackPreview)
            coroutineContext.ensureActive()
            if (!ownsRound(index, round)) return@launch
            startListeningWithRandomStartPoint(round, index, cover)
            val (_, error) = expectFinished()
            _uiState.value = if (error != null) {
                GameUiState.Error(UserErrorMapper.map(error, UserErrorContext.PLAYBACK))
            } else {
                GameUiState.Guessing(index, gameManager.currentRoundOptions, cover)
            }
        }
        roundJob = job
        job.start()
    }

    private fun startListeningWithRandomStartPoint(round: GameRound, index: Int, cover: PreparedCover?) {
        val start = if (gameManager.config.snippetLength == AppConsts.MAX_SNIPPET_LENGTH_IN_SECONDS * 1_000L) {
            0L
        } else {
            Random.nextLong(0L, AppConsts.MAX_SNIPPET_LENGTH_IN_SECONDS * 1_000L - gameManager.config.snippetLength)
        }
        _uiState.value = GameUiState.ListeningPreview(index, cover)
        audioPlayer.play(round.trackPreview.trackPreviewUrl, start, start + gameManager.config.snippetLength)
    }

    private suspend fun expectFinished() = combine(audioPlayer.snippetFinished, audioPlayer.error) { finished, error ->
            finished to error
        }.first { (finished, error) -> finished || error != null }


    fun checkAnswer(userAnswer: Track) {
        val guessing = _uiState.value as? GameUiState.Guessing ?: return
        val isGuessed = gameManager.checkAnswer(userAnswer)
        _uiState.value = GameUiState.Answer(
            currentRoundIndex, gameManager.getCurrentRound().track, isGuessed, guessing.preparedCover,
        )
        gameManager.nextRound()
    }

    private fun ownsRound(index: Int, round: GameRound): Boolean {
        if (currentRoundIndex != index) return false
        return try {
            gameManager.getCurrentRound() === round
        } catch (_: IndexOutOfBoundsException) {
            false
        }
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        roundJob?.cancel()
        _uiState.value = GameUiState.GameOver(score)
        audioPlayer.stop()
        super.onCleared()
    }
}
