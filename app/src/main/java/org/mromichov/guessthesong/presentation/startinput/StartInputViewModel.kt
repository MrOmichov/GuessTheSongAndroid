package org.mromichov.guessthesong.presentation.startinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import org.mromichov.guessthesong.domain.game.GameConfig
import org.mromichov.guessthesong.domain.game.GameManager
import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import javax.inject.Inject

class StartInputViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val gameManager: GameManager,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StartInputUiState>(StartInputUiState.Menu)
    val uiState = _uiState.asStateFlow()

    private var operationJob: Job? = null
    private var requestVersion = 0L

    fun openImportForm() {
        if (_uiState.value == StartInputUiState.Menu || _uiState.value is StartInputUiState.SelectionError) {
            _uiState.value = StartInputUiState.ImportForm
        }
    }

    fun cancelImportForm() {
        when (_uiState.value) {
            StartInputUiState.ImportForm,
            is StartInputUiState.ImportError,
            -> _uiState.value = StartInputUiState.Menu

            is StartInputUiState.PlaylistAlreadySavedConflict -> dismissPlaylistConflict()
            StartInputUiState.Importing -> cancelOperationToMenu()
            else -> Unit
        }
    }

    fun loadPlaylist(url: String) {
        if (!canLoadPlaylist(url)) return
        _uiState.value = StartInputUiState.Importing
        launchOperation(
            onFailure = { StartInputUiState.ImportError(it.message ?: "Ошибка загрузки плейлиста") },
        ) { request ->
            val fetchedPlaylist = playlistRepository.fetchPlaylistByUrl(url).getOrThrow()
            val existing = playlistRepository
                .findLocalPlaylistByTitle(fetchedPlaylist.content.title)
                .getOrThrow()
            if (existing != null) {
                publish(request, StartInputUiState.PlaylistAlreadySavedConflict(existing, fetchedPlaylist))
            } else {
                val saved = playlistRepository.savePlaylist(
                    fetchedPlaylist.content,
                    fetchedPlaylist.source.provider,
                ).getOrThrow()
                publish(request, StartInputUiState.Selected(saved.content))
            }
        }
    }

    fun selectSavedPlaylist(id: Long) {
        if ((_uiState.value != StartInputUiState.Menu && _uiState.value !is StartInputUiState.SelectionError) ||
            hasUnfinishedOperation()
        ) return
        _uiState.value = StartInputUiState.SelectingSavedPlaylist(id)
        launchOperation(
            onFailure = { StartInputUiState.SelectionError(it.message ?: "Не удалось открыть плейлист") },
        ) { request ->
            val localPlaylist = playlistRepository.getLocalPlaylist(id).getOrThrow()
            publish(request, StartInputUiState.Selected(localPlaylist.content))
        }
    }

    fun prepareGame() {
        if (hasUnfinishedOperation()) return
        val playlist = when (val state = _uiState.value) {
            is StartInputUiState.Selected -> state.playlist
            is StartInputUiState.PreparationError -> state.playlist
            else -> return
        }
        _uiState.value = StartInputUiState.Preparing(playlist)
        launchOperation(
            onFailure = {
                StartInputUiState.PreparationError(
                    message = it.message ?: "Ошибка подготовки игры",
                    playlist = playlist,
                )
            },
        ) { request ->
            gameManager.reset()
            val config = GameConfig(
                settingsRepository.roundsNumber.first(),
                settingsRepository.snippetLength.first(),
            )
            val operationContext = currentCoroutineContext()
            gameManager.prepareRounds(playlist, config) { completed, total ->
                if (request == requestVersion && operationContext.isActive) {
                    _uiState.value = StartInputUiState.Preparing(playlist, completed, total)
                }
            }
            publish(request, StartInputUiState.ReadyToPlay(playlist))
        }
    }

    fun changePlaylist() {
        if (_uiState.value is StartInputUiState.Selected ||
            _uiState.value is StartInputUiState.PreparationError
        ) {
            _uiState.value = StartInputUiState.Menu
        }
    }

    /** Consumes the one-shot navigation state before the caller opens GameRoute. */
    fun consumeReadyToPlay(): Boolean {
        val state = _uiState.value as? StartInputUiState.ReadyToPlay ?: return false
        _uiState.value = StartInputUiState.Selected(state.playlist)
        return true
    }

    fun handleBack(): Boolean {
        when (val state = _uiState.value) {
            is StartInputUiState.PlaylistAlreadySavedConflict -> dismissPlaylistConflict()
            StartInputUiState.ImportForm, is StartInputUiState.ImportError -> cancelImportForm()
            StartInputUiState.Importing, is StartInputUiState.SelectingSavedPlaylist -> cancelOperationToMenu()
            is StartInputUiState.Preparing -> cancelPreparation(state.playlist)
            StartInputUiState.CancellingToMenu, is StartInputUiState.CancellingPreparation -> Unit
            else -> return false
        }
        return true
    }

    fun shouldHandleBack(state: StartInputUiState): Boolean = when (state) {
        is StartInputUiState.PlaylistAlreadySavedConflict,
        StartInputUiState.ImportForm,
        is StartInputUiState.ImportError,
        StartInputUiState.Importing,
        is StartInputUiState.SelectingSavedPlaylist,
        is StartInputUiState.Preparing,
        StartInputUiState.CancellingToMenu,
        is StartInputUiState.CancellingPreparation,
        -> true

        else -> false
    }

    fun canLoadPlaylist(url: String): Boolean =
        url.isNotBlank() && !hasUnfinishedOperation() &&
            (_uiState.value == StartInputUiState.ImportForm || _uiState.value is StartInputUiState.ImportError)

    fun canPrepareGame(): Boolean =
        !hasUnfinishedOperation() &&
            (_uiState.value is StartInputUiState.Selected || _uiState.value is StartInputUiState.PreparationError)

    fun replacePlaylist(existingPlaylistId: Long, playlistContent: PlaylistContent) {
        val state = _uiState.value as? StartInputUiState.PlaylistAlreadySavedConflict ?: return
        if (hasUnfinishedOperation() || state.alreadyExistingPlaylist.id != existingPlaylistId ||
            state.fetchedPlaylist.content != playlistContent
        ) return
        saveConflictPlaylist {
            playlistRepository.replacePlaylist(
                existingPlaylistId,
                playlistContent,
                state.fetchedPlaylist.source.provider,
            )
        }
    }

    fun addPlaylist(playlistContent: PlaylistContent) {
        val state = _uiState.value as? StartInputUiState.PlaylistAlreadySavedConflict ?: return
        if (hasUnfinishedOperation() || state.fetchedPlaylist.content != playlistContent) return
        saveConflictPlaylist {
            playlistRepository.savePlaylist(playlistContent, state.fetchedPlaylist.source.provider)
        }
    }

    fun dismissPlaylistConflict() {
        if (_uiState.value is StartInputUiState.PlaylistAlreadySavedConflict) {
            _uiState.value = StartInputUiState.ImportForm
        }
    }

    private fun saveConflictPlaylist(save: suspend () -> Result<LocalPlaylist>) {
        _uiState.value = StartInputUiState.Importing
        launchOperation(
            onFailure = { StartInputUiState.ImportError(it.message ?: "Ошибка сохранения плейлиста") },
        ) { request ->
            val saved = save().getOrThrow()
            publish(request, StartInputUiState.Selected(saved.content))
        }
    }

    private fun cancelPreparation(playlist: PlaylistContent) {
        val job = operationJob
        if (job == null || job.isCompleted) {
            _uiState.value = StartInputUiState.Selected(playlist)
            return
        }
        requestVersion++
        _uiState.value = StartInputUiState.CancellingPreparation(playlist)
        job.cancel()
        viewModelScope.launch {
            job.join()
            if (_uiState.value == StartInputUiState.CancellingPreparation(playlist)) {
                _uiState.value = StartInputUiState.Selected(playlist)
            }
        }
    }

    private fun cancelOperationToMenu() {
        val job = operationJob
        if (job == null || job.isCompleted) {
            requestVersion++
            _uiState.value = StartInputUiState.Menu
            return
        }
        requestVersion++
        _uiState.value = StartInputUiState.CancellingToMenu
        job.cancel()
        viewModelScope.launch {
            job.join()
            if (_uiState.value == StartInputUiState.CancellingToMenu) {
                _uiState.value = StartInputUiState.Menu
            }
        }
    }

    private fun launchOperation(
        onFailure: (Exception) -> StartInputUiState,
        block: suspend (request: Long) -> Unit,
    ) {
        if (hasUnfinishedOperation()) return
        val request = ++requestVersion
        // Assign before start: Main.immediate can otherwise finish the coroutine before the Job is stored.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                block(request)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (request == requestVersion && currentCoroutineContext().isActive) {
                    _uiState.value = onFailure(error)
                }
            }
        }
        operationJob = job
        job.start()
    }

    private fun hasUnfinishedOperation(): Boolean = operationJob?.isCompleted == false

    private suspend fun publish(request: Long, state: StartInputUiState) {
        currentCoroutineContext().ensureActive()
        if (request == requestVersion) _uiState.value = state
    }
}
