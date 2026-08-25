package org.mromichov.guessthesong.presentation.startinput

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import javax.inject.Inject

class StartInputViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val gameManager: GameManager
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
                        gameManager.prepareRounds(playlist)
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