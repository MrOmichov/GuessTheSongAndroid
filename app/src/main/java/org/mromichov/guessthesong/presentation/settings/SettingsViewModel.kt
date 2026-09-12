package org.mromichov.guessthesong.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val uiState = combine(
        settingsRepository.minimalRoundsNumber,
        settingsRepository.snippetLength
    ) { roundsNumber, snippetLength ->
        SettingsUiState.Success(roundsNumber, (snippetLength / 1000).toInt())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState.Loading
    )

    fun changeRoundsNumber(roundsNumber: Int) {
        viewModelScope.launch {
            settingsRepository.setMinimalRoundsNumber(roundsNumber)
        }
    }

    fun changeSnippetLength(snippetLength: Int) {
        viewModelScope.launch {
            settingsRepository.setSnippetLength((snippetLength * 1000).toLong())
        }
    }
}