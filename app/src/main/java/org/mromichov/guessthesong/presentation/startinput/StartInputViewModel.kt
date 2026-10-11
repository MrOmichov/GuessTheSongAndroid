package org.mromichov.guessthesong.presentation.startinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.game.GameConfig
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class StartInputViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val gameManager: GameManager,

    // TODO убрать SettingsRepository, давать доступ только к флоу
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<StartInputUiState>(StartInputUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // TODO тоже странная вещь прямо во ViewModel
    private lateinit var playlist: Playlist

    fun loadPlaylist(url: String) {
        if (!canLoadPlaylist(url)) return
        _uiState.value = StartInputUiState.Loading
        viewModelScope.launch {
            playlistRepository.getPlaylistByUrl(url)
                .onSuccess { playlist ->
                    this@StartInputViewModel.playlist = playlist
                    _uiState.value = StartInputUiState.Loaded(playlist)
                }
                .onFailure { error ->
                    _uiState.value = StartInputUiState.Error(error.message ?: "Ошибка загрузки плейлиста")
                }

        }
    }

    fun prepareGame() {
        if (uiState.value is StartInputUiState.Loaded) {
            _uiState.value = StartInputUiState.Preparing(playlist)
            viewModelScope.launch {
                try {
                    gameManager.reset()
                    gameManager.prepareRounds(
                        playlist,
                        GameConfig(
                            settingsRepository.roundsNumber.first(),
                            settingsRepository.snippetLength.first()
                        )
                    )
                    _uiState.value = StartInputUiState.ReadyToPlay(playlist)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _uiState.value = StartInputUiState.Error(error.message ?: "Ошибка загрузки плейлиста")
                }
            }
        }
    }

    fun onGameStarted() {
        val state = uiState.value
        if (state is StartInputUiState.ReadyToPlay) {
            _uiState.value = StartInputUiState.Loaded(state.playlist)
        }
    }

    fun canLoadPlaylist(url: String): Boolean {
        if (url.isBlank()) return false
        val state = _uiState.value
        return state is StartInputUiState.Loaded || state is StartInputUiState.Idle || state is StartInputUiState.Error
    }
}