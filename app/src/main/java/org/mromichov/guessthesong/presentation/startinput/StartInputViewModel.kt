package org.mromichov.guessthesong.presentation.startinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.game.GameConfig
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import javax.inject.Inject

class StartInputViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val gameManager: GameManager,

    // TODO убрать SettingsRepository, давать доступ только к флоу
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<StartInputUiState>(StartInputUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun loadPlaylist(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = StartInputUiState.Loading
            playlistRepository.getPlaylistByUrl(url)
                .onSuccess { playlist ->
                    try {
                        gameManager.reset()
                        gameManager.prepareRounds(
                            playlist,
                            GameConfig(
                                settingsRepository.roundsNumber.first(),
                                settingsRepository.snippetLength.first()
                            )
                        )
                        _uiState.value = StartInputUiState.Success(playlist)
                    } catch (error: Exception) {
                        _uiState.value = StartInputUiState.Error(error.message ?: "Ошибка загрузки плейлиста")
                    }
                }
                .onFailure { error ->
                    _uiState.value = StartInputUiState.Error(error.message ?: "Ошибка загрузки плейлиста")
                }

        }
    }
}