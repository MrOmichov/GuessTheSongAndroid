package org.mromichov.guessthesong.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.core.AppConsts.Companion.MAX_ROUNDS_NUMBER
import org.mromichov.guessthesong.core.AppConsts.Companion.MAX_SNIPPET_LENGTH_IN_SECONDS
import org.mromichov.guessthesong.domain.repository.SettingsRepository
import javax.inject.Inject

// ViewModel видит snippetLength в секундах
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val uiState = combine(
        settingsRepository.roundsNumber,
        settingsRepository.snippetLength
    ) { roundsNumber, snippetLength ->
        SettingsUiState.Success(roundsNumber, (snippetLength / 1000).toInt())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState.Loading
    )

    fun changeRoundsNumber(roundsNumber: Int): String? {
        if (roundsNumber !in 1..MAX_ROUNDS_NUMBER) {
            return "Количество раундов должно быть от 1 до $MAX_ROUNDS_NUMBER"
        }
        viewModelScope.launch {
            settingsRepository.setRoundsNumber(roundsNumber)
        }
        return null
    }

    fun changeSnippetLength(snippetLength: Int): String? {
        if (snippetLength !in 1..MAX_SNIPPET_LENGTH_IN_SECONDS) {
            return "Длина отрывка должна быть от 1 до ${MAX_SNIPPET_LENGTH_IN_SECONDS - 1} с"
        }
        viewModelScope.launch {
            settingsRepository.setSnippetLength((snippetLength * 1000).toLong())
        }
        return null
    }
}
