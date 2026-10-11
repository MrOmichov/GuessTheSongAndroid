package org.mromichov.guessthesong.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.presentation.sharedcomponent.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
            )
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
                    val roundsNumber = state.roundsNumber
                    val snippetLength = state.snippetLength
                    AppContainer {
                        Row {
                            Text(
                                text = "Количество раундов"
                            )
                            OutlinedTextField(
                                value = roundsNumber.toString(),
                                onValueChange = { newValue ->
                                    val fieldInput = newValue.toIntOrNull()
                                    val error = if (fieldInput == null) {
                                        "Введите целое число"
                                    } else {
                                        viewModel.changeRoundsNumber(fieldInput)
                                    }
                                    if (error != null) {
                                        scope.launch { snackbarHostState.showSnackbar(error) }
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
                                    text = "Длина отрывка, с"
                                )
                                OutlinedTextField(
                                    value = snippetLength.toString(),
                                    onValueChange = { newValue ->
                                        val fieldInput = newValue.toIntOrNull()
                                        val error = if (fieldInput == null) {
                                            "Введите целое число"
                                        } else {
                                            viewModel.changeSnippetLength(fieldInput)
                                        }
                                        if (error != null) {
                                            scope.launch { snackbarHostState.showSnackbar(error) }
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
