package org.mromichov.guessthesong.presentation.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mromichov.guessthesong.presentation.sharedcomponent.AppContainer

@Composable
fun GameScreen(viewModel: GameViewModel, onEnd: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is GameUiState.ListeningPreview -> {
                    ListeningPreviewContent(viewModel)
                }
                is GameUiState.Guessing -> {
                    val state = uiState as GameUiState.Guessing
                    Text("${state.currentRoundNumber}")

                    LazyColumn {
                        items(state.options.size) {
                            OutlinedButton(
                                onClick = { viewModel.checkAnswer(state.options[it]) }
                            ) {
                                Text(text = "${state.options[it].title} от ${state.options[it].artist}")
                            }
                        }
                    }
                }
                is GameUiState.Answer -> {
                    val state = uiState as GameUiState.Answer
                    Column {
                        Text("${state.currentRoundNumber}")
                        AppContainer {
                            Column {
                                Text("Правильный ответ: ${state.answer.title} от ${state.answer.artist}")
                                Text(
                                    if (state.isGuessed)
                                        "Ответ верный"
                                    else
                                        "Ответ неверный"
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.startRoundOrGameOver() }
                    ) {
                        Text("Дальше")
                    }
                }
                is GameUiState.GameOver -> {
                    Text(
                        text = "Game Over",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    OutlinedButton(
                        onClick = onEnd
                    ) {
                       Text("В главное меню")
                    }
                }

            }
        }
    }
}

@Composable
private fun ListeningPreviewContent(viewModel: GameViewModel) {
    val currentMs by viewModel.audioPlayer.currentMs.collectAsState()
    val durationMs by viewModel.audioPlayer.durationMs.collectAsState()
    val targetProgress = if (durationMs > 0L) {
        (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 60, easing = LinearEasing),
        label = "smooth_progress"
    )

    Column {
        Text("${viewModel.currentRoundNumber}")
        AppContainer {
            LinearProgressIndicator(
                progress = { animatedProgress },
            )
            Text(text = "$currentMs / $durationMs")
        }
    }

}