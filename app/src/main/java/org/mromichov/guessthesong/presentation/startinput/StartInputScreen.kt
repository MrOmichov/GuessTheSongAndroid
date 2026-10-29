package org.mromichov.guessthesong.presentation.startinput

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
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
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(enabled = viewModel.shouldHandleBack(uiState)) {
        viewModel.handleBack()
    }

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.uiState.collect { state ->
                if (state is StartInputUiState.ReadyToPlay) startGame()
            }
        }
    }

    Scaffold(
        topBar = {
            TopGuessTheSongAppBar {
                IconButton(
                    onClick = onOpenSettings,
                    enabled = uiState !is StartInputUiState.Importing &&
                        uiState !is StartInputUiState.SelectingSavedPlaylist &&
                        uiState !is StartInputUiState.Preparing &&
                        uiState !is StartInputUiState.CancellingToMenu &&
                        uiState !is StartInputUiState.CancellingPreparation,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Настройки",
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            contentAlignment = Alignment.Center,
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
    onOpenPlaylists: () -> Unit,
) {
    var urlText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState) {
            StartInputUiState.Menu -> MenuContent(viewModel, onOpenPlaylists)
            is StartInputUiState.SelectionError -> {
                ErrorContent(uiState.message)
                Spacer(Modifier.height(16.dp))
                MenuContent(viewModel, onOpenPlaylists)
            }

            StartInputUiState.ImportForm,
            StartInputUiState.Importing,
            is StartInputUiState.ImportError,
            is StartInputUiState.PlaylistAlreadySavedConflict,
            -> ImportContent(viewModel, uiState, urlText, { urlText = it })

            is StartInputUiState.Selected -> SelectedPlaylistContent(
                viewModel = viewModel,
                playlistTitle = uiState.playlist.title,
                showError = null,
                preparing = false,
            )

            is StartInputUiState.PreparationError -> SelectedPlaylistContent(
                viewModel = viewModel,
                playlistTitle = uiState.playlist.title,
                showError = uiState.message,
                preparing = false,
            )

            is StartInputUiState.Preparing -> SelectedPlaylistContent(
                viewModel = viewModel,
                playlistTitle = uiState.playlist.title,
                showError = null,
                preparing = true,
                completed = uiState.completed,
                total = uiState.total,
            )

            is StartInputUiState.CancellingPreparation -> SelectedPlaylistContent(
                viewModel = viewModel,
                playlistTitle = uiState.playlist.title,
                showError = null,
                preparing = true,
                cancellationPending = true,
            )

            StartInputUiState.CancellingToMenu,
            is StartInputUiState.SelectingSavedPlaylist,
            is StartInputUiState.ReadyToPlay,
            -> OperationProgressContent(
                label = when (uiState) {
                    StartInputUiState.CancellingToMenu -> "Отмена…"
                    is StartInputUiState.SelectingSavedPlaylist -> "Открываем плейлист…"
                    is StartInputUiState.ReadyToPlay -> "Игра готова…"
                },
            )
        }

        if (uiState is StartInputUiState.PlaylistAlreadySavedConflict) {
            val state = uiState
            PlaylistAlreadySavedDialog(
                state.alreadyExistingPlaylist.content.title,
                onDismissRequest = viewModel::dismissPlaylistConflict,
                onReplace = {
                    viewModel.replacePlaylist(state.alreadyExistingPlaylist.id, state.fetchedPlaylist.content)
                },
                onAdd = { viewModel.addPlaylist(state.fetchedPlaylist.content) },
            )
        }
    }
}

@Composable
private fun MenuContent(viewModel: StartInputViewModel, onOpenPlaylists: () -> Unit) {
    AppButton(
        icon = painterResource(R.drawable.download),
        text = "Загрузить плейлист",
        onClick = viewModel::openImportForm,
    )
    Spacer(Modifier.height(16.dp))
    AppButton(
        icon = painterResource(R.drawable.playlist_placeholder),
        text = "Сохранённые плейлисты",
        onClick = onOpenPlaylists,
    )
}

@Composable
private fun ImportContent(
    viewModel: StartInputViewModel,
    state: StartInputUiState,
    urlText: String,
    onUrlChange: (String) -> Unit,
) {
    InputUrlContainer(urlText, onUrlChange, state)
    if (state == StartInputUiState.Importing) {
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(modifier = Modifier.width(300.dp))
    }
    if (state is StartInputUiState.ImportError) {
        Spacer(Modifier.height(12.dp))
        ErrorContent(state.message)
    }
    Spacer(Modifier.height(16.dp))
    AppButton(
        icon = painterResource(if (state == StartInputUiState.Importing) R.drawable.sync else R.drawable.download),
        text = if (state == StartInputUiState.Importing) "Загрузка…" else "Загрузить",
        onClick = { viewModel.loadPlaylist(urlText) },
        enabled = viewModel.canLoadPlaylist(urlText),
    )
    Spacer(Modifier.height(12.dp))
    AppButton(
        icon = painterResource(R.drawable.arrow_back),
        text = if (state == StartInputUiState.Importing) "Отменить загрузку" else "Отмена",
        onClick = viewModel::cancelImportForm,
    )
}

@Composable
private fun SelectedPlaylistContent(
    viewModel: StartInputViewModel,
    playlistTitle: String,
    showError: String?,
    preparing: Boolean,
    cancellationPending: Boolean = false,
    completed: Int = 0,
    total: Int = 0,
) {
    Text(
        text = playlistTitle,
        modifier = Modifier.width(300.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
    )
    if (showError != null) {
        Spacer(Modifier.height(12.dp))
        ErrorContent(showError)
    }
    if (preparing) {
        Spacer(Modifier.height(16.dp))
        if (!cancellationPending && total > 0) {
            LinearProgressIndicator(progress = { completed.toFloat() / total }, modifier = Modifier.width(300.dp))
            Spacer(Modifier.height(12.dp))
            Text("Подготавливаем игру… ${completed * 100 / total}% ($completed/$total)")
        } else {
            OperationProgressContent(
                label = if (cancellationPending) "Завершаем отмену…" else "Подготавливаем игру…",
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    AppButton(
        icon = painterResource(R.drawable.play),
        text = if (showError != null) "Повторить подготовку" else "Начать игру",
        onClick = viewModel::prepareGame,
        enabled = !preparing && viewModel.canPrepareGame(),
    )
    Spacer(Modifier.height(12.dp))
    AppButton(
        icon = painterResource(R.drawable.playlist_placeholder),
        text = "Сменить плейлист",
        onClick = viewModel::changePlaylist,
        enabled = !preparing,
    )
}

@Composable
private fun OperationProgressContent(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(label)
    }
}

@Composable
private fun ErrorContent(message: String) {
    ErrorContainer {
        Text(
            text = message,
            modifier = Modifier
                .width(300.dp)
                .padding(12.dp),
        )
    }
}
