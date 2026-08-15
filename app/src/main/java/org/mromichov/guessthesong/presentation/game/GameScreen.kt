package org.mromichov.guessthesong.presentation.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GameScreen(viewModel: GameViewModel, onEnd: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState is GameUiState.Idle) {
                val playlist = (uiState as GameUiState.Idle).playlist
                val tracks = playlist.tracks
                val countTracks = tracks.count()

                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.headlineSmall
                )
                LazyColumn {
                    items(countTracks) { index ->
                        Text("${tracks[index].title} от ${tracks[index].artist}")
                    }
                }
            }
        }
    }
}