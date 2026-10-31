package org.mromichov.guessthesong.presentation.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
import org.mromichov.guessthesong.presentation.sharedcomponent.AppAlertDialog
import org.mromichov.guessthesong.presentation.sharedcomponent.AppButton
import org.mromichov.guessthesong.presentation.sharedcomponent.ErrorContainer
import org.mromichov.guessthesong.presentation.sharedcomponent.TopGuessTheSongAppBar

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    back: () -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onEmptyListImport: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.showDeleteErrorSnackbar) {
        if (viewModel.showDeleteErrorSnackbar) {
            scope.launch {
                snackbarHostState.showSnackbar("Не удалось удалить плейлист")
            }
        }
    }

    Scaffold(
        topBar = {
            TopGuessTheSongAppBar(
                back = back,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            if (uiState is PlaylistsUiState.DeletePlaylistIntent) {
                val state = uiState as PlaylistsUiState.DeletePlaylistIntent
                AppAlertDialog(
                    dialogTitle = "Вы хотите удалить этот плейлист?",
                    dialogText = "",
                    onDismissRequest = { viewModel.backToLoaded(state.playlistsLoaded) },
                    dismissButton = { AppButton(
                        icon = painterResource(R.drawable.arrow_back),
                        onClick = { viewModel.backToLoaded(state.playlistsLoaded) },
                        text = "Отмена"
                    ) },
                    confirmButton = { AppButton(
                        icon = painterResource(R.drawable.delete),
                        onClick = { viewModel.deletePlaylist(state.id) },
                        text = "Да"
                    ) },
                    icon = painterResource(R.drawable.delete),
                )
            }

            when (uiState) {
                is PlaylistsUiState.Error -> {
                    ErrorContainer {
                        Text(text = (uiState as PlaylistsUiState.Error).message)
                    }
                    AppButton(
                        icon = painterResource(R.drawable.sync),
                        text = "Перезагрузить",
                        onClick = { viewModel.loadPlaylists() }
                    )
                }

                is PlaylistsUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is PlaylistsUiState.Loaded -> PlaylistsContent(
                    playlists = (uiState as PlaylistsUiState.Loaded).playlists,
                    onPlaylistSelected = onPlaylistSelected,
                    onLongPlaylistClick = { id -> viewModel.showDeleteDialog(id, (uiState as PlaylistsUiState.Loaded).playlists) }
                )

                is PlaylistsUiState.DeletePlaylistIntent -> PlaylistsContent(
                    playlists = (uiState as PlaylistsUiState.DeletePlaylistIntent).playlistsLoaded,
                    onPlaylistSelected = {},
                    onLongPlaylistClick = {}
                )

                PlaylistsUiState.LoadedEmpty -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    ) {
                        Text("Сохранённых плейлистов пока нет", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        AppButton(
                            icon = painterResource(R.drawable.download),
                            text = "Загрузить плейлист",
                            onClick = onEmptyListImport,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsContent(
    playlists: List<LocalPlaylist>,
    onPlaylistSelected: (Long) -> Unit,
    onLongPlaylistClick: (Long) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(
            items = playlists,
            key = { playlist -> playlist.id },
        ) { playlist ->
            PlaylistCard(playlist = playlist, onClick = { onPlaylistSelected(playlist.id) }, onLongClick = { onLongPlaylistClick(playlist.id) })
        }
    }
}

@Composable
internal fun PlaylistCard(playlist: LocalPlaylist, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val coverFile = remember(playlist.coverFileName, context.filesDir) {
        playlist.coverFileName
            ?.takeIf { it.isNotBlank() && File(it).name == it }
            ?.let { File(File(context.filesDir, "playlist-covers"), it) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
    ) {
        Column {
            AsyncImage(
                model = coverFile,
                contentDescription = "Обложка плейлиста ${playlist.content.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                placeholder = painterResource(R.drawable.playlist_placeholder),
                error = painterResource(R.drawable.playlist_placeholder),
                fallback = painterResource(R.drawable.playlist_placeholder),
                contentScale = ContentScale.Crop
            )
            Text(
                text = playlist.content.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 36.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        val providerIcon = when (playlist.provider) {
            PlaylistProvider.SPOTIFY -> R.drawable.spotify_logo
            PlaylistProvider.YANDEX_MUSIC -> R.drawable.yandex_music_logo
            null -> null
        }
        val providerDescription = when (playlist.provider) {
            PlaylistProvider.SPOTIFY -> "Spotify"
            PlaylistProvider.YANDEX_MUSIC -> "Яндекс Музыка"
            null -> null
        }
        if (providerIcon != null && providerDescription != null) {
            Icon(
                painter = painterResource(providerIcon),
                contentDescription = providerDescription,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(20.dp),
                tint = Color.Unspecified
            )
        }
    }
}
