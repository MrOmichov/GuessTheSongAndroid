package org.mromichov.guessthesong.presentation.startinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.game.GameConfig
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.PlaylistContent
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
    private lateinit var playlist: PlaylistContent

    fun loadPlaylist(url: String) {
        if (!canLoadPlaylist(url)) return
        _uiState.value = StartInputUiState.Loading
        viewModelScope.launch {
            try {
                val importedPlaylist = playlistRepository.fetchPlaylistByUrl(url).getOrThrow()
                val savedPlaylist = playlistRepository.findLocalPlaylistByTitle(importedPlaylist.title).getOrThrow()
                if (savedPlaylist != null) {
                    _uiState.value = StartInputUiState.PlaylistAlreadySavedConflict(
                        savedPlaylistId = savedPlaylist.id.toLong(),
                        savedPlaylistTitle = savedPlaylist.title,
                        savedPlaylist = importedPlaylist,
                    )
                } else {
                    playlist = playlistRepository.savePlaylist(importedPlaylist).getOrThrow()
                    _uiState.value = StartInputUiState.Loaded(playlist)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
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

    fun replacePlaylist(existingPlaylistId: Long, playlist: PlaylistContent) {
        val state = _uiState.value as? StartInputUiState.PlaylistAlreadySavedConflict ?: return
        if (state.savedPlaylistId != existingPlaylistId || state.savedPlaylist != playlist) return
        saveConflictPlaylist { playlistRepository.replacePlaylist(existingPlaylistId, playlist) }
    }

    fun addPlaylist(playlist: PlaylistContent) {
        val state = _uiState.value as? StartInputUiState.PlaylistAlreadySavedConflict ?: return
        if (state.savedPlaylist != playlist) return
        saveConflictPlaylist { playlistRepository.savePlaylist(playlist) }
    }

    fun dismissPlaylistConflict() {
        if (_uiState.value is StartInputUiState.PlaylistAlreadySavedConflict) {
            _uiState.value = StartInputUiState.Idle
        }
    }

    private fun saveConflictPlaylist(save: suspend () -> Result<PlaylistContent>) {
        _uiState.value = StartInputUiState.Loading
        viewModelScope.launch {
            try {
                playlist = save().getOrThrow()
                _uiState.value = StartInputUiState.Loaded(playlist)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.value = StartInputUiState.Error(error.message ?: "Ошибка сохранения плейлиста")
            }
        }
    }

}
