package org.mromichov.guessthesong.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mromichov.guessthesong.presentation.sharedcomponent.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    back: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {}
            )
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
                    var roundsNumber by remember { mutableIntStateOf(state.roundsNumber) }
                    var snippetLength by remember { mutableIntStateOf(state.snippetLength) }
                    AppContainer {
                        Row {
                            Text(
                                text = "Количество раундов"
                            )
                            OutlinedTextField(
                                value = roundsNumber.toString(),
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        roundsNumber = newValue.toInt()
                                        viewModel.changeRoundsNumber(roundsNumber)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                singleLine = true
                            )
                        }
                        AppContainer {
                            Row {
                                Text(
                                    text = "Длина отрывка"
                                )
                                OutlinedTextField(
                                    value = snippetLength.toString(),
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                            snippetLength = newValue.toInt()
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number
                                    ),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
