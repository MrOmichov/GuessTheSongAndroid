package org.mromichov.guessthesong.presentation.startinput

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartInputScreen(viewModel: StartInputViewModel, onStartGame: () -> Unit) {
    var urlText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Guess the Song",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Введите ссылку, чтобы начать игру",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    modifier = Modifier.width(300.dp),
                    value = urlText,
                    onValueChange = { newValue -> urlText = newValue },
                    singleLine = true,
                    isError = uiState is StartInputUiState.Error,
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {viewModel.loadPlaylist(url = urlText)},
                    enabled = urlText.isNotBlank() && uiState !is StartInputUiState.Loading,
                ) {
                    when (uiState) {
                        is StartInputUiState.Error -> Text("Загрузить плейлист")
                        StartInputUiState.Idle -> Text("Загрузить плейлист")
                        StartInputUiState.Loading -> CircularProgressIndicator()
                        is StartInputUiState.Success -> Text("Загрузить плейлист")
                    }
                }

                if (uiState is StartInputUiState.Error) {
                    Text(
                        text = (uiState as StartInputUiState.Error).message,
                        modifier = Modifier.width(300.dp),
                    )
                } else if (uiState is StartInputUiState.Success) {
                    OutlinedButton(
                        onClick = onStartGame,
                    ) {
                        Text("Начать игру")
                    }
                    Text(
                        text = (uiState as StartInputUiState.Success).playlist.title,
                        modifier = Modifier.width(300.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}