package org.mromichov.guessthesong.presentation.startinput

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mromichov.guessthesong.App
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.presentation.sharedcomponent.AppButton
import org.mromichov.guessthesong.presentation.startinput.component.InputUrlContainer

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
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InputUrlContainer(
                    urlText,
                    { newValue ->
                        urlText = newValue
                    },
                    uiState
                )

                Spacer(Modifier.padding(16.dp))

                AppButton(
                    icon = painterResource(
                        when (uiState) {
                            StartInputUiState.Loading -> R.drawable.sync
                            else -> R.drawable.download
                        }
                    ),
                    text = when (uiState) {
                        StartInputUiState.Loading -> ""
                        else -> "Загрузить плейлист"
                    },
                    onClick = { viewModel.loadPlaylist(url = urlText) },
                    enabled = urlText.isNotBlank() && uiState !is StartInputUiState.Loading
                )

                Spacer(Modifier.padding(16.dp))

                if (uiState is StartInputUiState.Error) {
                    Text(
                        text = (uiState as StartInputUiState.Error).message,
                        modifier = Modifier.width(300.dp),
                    )
                } else if (uiState is StartInputUiState.Success) {
                    AppButton(
                        icon = painterResource(R.drawable.play),
                        text = "Начать игру",
                        onClick = onStartGame,
                    )

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