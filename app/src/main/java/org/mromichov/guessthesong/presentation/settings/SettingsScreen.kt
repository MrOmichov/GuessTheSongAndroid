package org.mromichov.guessthesong.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.presentation.settings.component.SettingsIntInputField
import org.mromichov.guessthesong.presentation.sharedcomponent.AppContainer
import org.mromichov.guessthesong.presentation.sharedcomponent.TopGuessTheSongAppBar

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    back: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopGuessTheSongAppBar(back = back)
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            when (uiState) {
                SettingsUiState.Loading -> CircularProgressIndicator()
                is SettingsUiState.Success -> {
                    val state = uiState as SettingsUiState.Success
                    AppContainer {
                        Row {
                            Text(
                                text = "Количество раундов"
                            )
                            SettingsIntInputField(
                                initValue = state.roundsNumber.toString(),
                                changeSetting = viewModel::changeRoundsNumber,
                                showSnackbar = { error -> scope.launch { snackbarHostState.showSnackbar(error) } }
                            )
                        }
                    }
                    AppContainer {
                        Row {
                            Text(
                                text = "Длина отрывка, с"
                            )
                            SettingsIntInputField(
                                initValue = state.snippetLength.toString(),
                                changeSetting = viewModel::changeSnippetLength,
                                showSnackbar = { error -> scope.launch { snackbarHostState.showSnackbar(error) } }
                            )
                        }
                    }
                }
            }
        }
    }
}
