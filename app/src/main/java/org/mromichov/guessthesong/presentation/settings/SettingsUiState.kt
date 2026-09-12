package org.mromichov.guessthesong.presentation.settings

sealed class SettingsUiState {
    object Loading : SettingsUiState()

    // snippetLength - seconds
    data class Success(
        val roundsNumber: Int,
        val snippetLength: Int
    ) : SettingsUiState()
}
