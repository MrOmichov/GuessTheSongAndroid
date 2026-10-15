package org.mromichov.guessthesong.presentation.startinput

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.presentation.sharedcomponent.AppButton
import org.mromichov.guessthesong.presentation.sharedcomponent.ErrorContainer
import org.mromichov.guessthesong.presentation.sharedcomponent.TopGuessTheSongAppBar
import org.mromichov.guessthesong.presentation.startinput.component.InputUrlContainer
import org.mromichov.guessthesong.presentation.startinput.component.PlaylistAlreadySavedDialog

@Composable
fun StartInputScreen(
    viewModel: StartInputViewModel,
    startGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlaylists: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is StartInputUiState.ReadyToPlay) {
            viewModel.onGameStarted()
            startGame()
        }
    }

    Scaffold(
        topBar = {
            TopGuessTheSongAppBar {
                IconButton(
                    onClick = onOpenSettings,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings",
                    )
                }
            }
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
            StartInputContent(
                viewModel = viewModel,
                uiState = uiState,
                onOpenPlaylists = onOpenPlaylists,
            )
        }
    }
}

@Composable
fun StartInputContent(
    viewModel: StartInputViewModel,
    uiState: StartInputUiState,
    onOpenPlaylists: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var urlText by remember { mutableStateOf("") }
        InputUrlContainer(
            urlText,
            { newValue ->
                urlText = newValue
            },
            uiState
        )

        Spacer(Modifier.padding(16.dp))

        MenuContent(viewModel, uiState, onOpenPlaylists, urlText)

        Spacer(Modifier.padding(16.dp))

        if (uiState is StartInputUiState.PlaylistAlreadySavedConflict) {
            val state = uiState
            PlaylistAlreadySavedDialog(
                state.alreadyExistingPlaylist.content.title,
                onDismissRequest = viewModel::dismissPlaylistConflict,
                onReplace = {
                    viewModel.replacePlaylist(state.alreadyExistingPlaylist.id, state.fetchedPlaylist.content)
                },
                onAdd = { viewModel.addPlaylist(state.fetchedPlaylist.content) }
            )
        }

        if (uiState is StartInputUiState.Error) {
            ErrorContent(uiState)
        } else if (uiState is StartInputUiState.Loaded || uiState is StartInputUiState.Preparing || uiState is StartInputUiState.ReadyToPlay) {
            when (uiState) {
                is StartInputUiState.Loaded -> ContentWithLoadedPlaylist(
                    viewModel,
                    uiState.playlistContent.title,
                    true
                )

                is StartInputUiState.Preparing -> ContentWithLoadedPlaylist(
                    viewModel,
                    uiState.playlist.title,
                    false
                )

                else -> {}
            }
        }
    }
}

@Composable
private fun MenuContent(
    viewModel: StartInputViewModel,
    state: StartInputUiState,
    onOpenPlaylists: () -> Unit,
    urlText: String,
) {
    AppButton(
        icon = painterResource(
            when (state) {
                StartInputUiState.Loading -> R.drawable.sync
                else -> R.drawable.download
            }
        ),
        text = when (state) {
            StartInputUiState.Loading -> ""
            else -> "Загрузить плейлист"
        },
        onClick = { viewModel.loadPlaylist(url = urlText) },
        enabled = viewModel.canLoadPlaylist(url = urlText)
    )

    Spacer(Modifier.padding(16.dp))

    AppButton(
        icon = painterResource(R.drawable.playlist_placeholder),
        text = "Сохранённые плейлисты",
        onClick = { onOpenPlaylists() },
        enabled = state is StartInputUiState.Loaded || state is StartInputUiState.Error || state is StartInputUiState.Idle
    )
}

@Composable
private fun ContentWithLoadedPlaylist(
    viewModel: StartInputViewModel,
    playlistTitle: String,
    startEnabled: Boolean
) {
    AppButton(
        icon = painterResource(R.drawable.play),
        text = "Начать игру",
        onClick = { viewModel.prepareGame() },
        enabled = startEnabled
    )

    Text(
        text = playlistTitle,
        modifier = Modifier.width(300.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ErrorContent(state: StartInputUiState.Error) {
    ErrorContainer {
        Text(
            text = state.message,
            modifier = Modifier.width(300.dp),
        )
    }
}
