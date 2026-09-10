package org.mromichov.guessthesong.presentation.settings

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(
        val roundsNumber: Int,
        val snippetLength: Long
    ) : SettingsUiState()
}
