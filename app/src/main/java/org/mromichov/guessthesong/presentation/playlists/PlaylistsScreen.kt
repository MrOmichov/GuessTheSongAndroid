package org.mromichov.guessthesong.presentation.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.presentation.sharedcomponent.AppButton
import org.mromichov.guessthesong.presentation.sharedcomponent.ErrorContainer
import org.mromichov.guessthesong.presentation.sharedcomponent.TopGuessTheSongAppBar

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    back: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopGuessTheSongAppBar(
                back = back,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
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
                    viewModel,
                    playlists = (uiState as PlaylistsUiState.Loaded).playlists
                )

                PlaylistsUiState.LoadedEmpty -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Скоро тут появятся плейлисты", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PlaylistsContent(viewModel: PlaylistsViewModel, playlists: List<Playlist>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(playlists) { playlist ->
            PlaylistCard(playlist)
        }
    }
}

@Composable
private fun PlaylistCard(playlist: Playlist) {
    Column {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.playlist_placeholder),
                contentDescription = "Playlist ${playlist.id}"
            )
        }
        Text(playlist.title, style = MaterialTheme.typography.bodyMedium)
    }
}