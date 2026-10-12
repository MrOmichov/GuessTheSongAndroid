package org.mromichov.guessthesong.presentation.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.presentation.game.component.BiDirectionalProgressBar
import org.mromichov.guessthesong.presentation.sharedcomponent.AppButton
import org.mromichov.guessthesong.presentation.sharedcomponent.AppContainer
import org.mromichov.guessthesong.presentation.sharedcomponent.ErrorContainer

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
                is GameUiState.ListeningPreview -> ListeningPreviewContent(viewModel)
                is GameUiState.Guessing -> GuessingContent(viewModel, uiState as GameUiState.Guessing)
                is GameUiState.Answer -> AnswerContent(viewModel, uiState as GameUiState.Answer)
                is GameUiState.GameOver -> GameOverContent(uiState as GameUiState.GameOver, onEnd)
                is GameUiState.Error -> ErrorContent(uiState as GameUiState.Error, onEnd)
            }
        }
    }
}

@Composable
private fun ListeningPreviewContent(viewModel: GameViewModel) {
    val currentMs by viewModel.currentMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val targetProgress = if (durationMs > 0L) {
        (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatableProgress = remember { Animatable(targetProgress) }
    LaunchedEffect(targetProgress) {
        animatableProgress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 60, easing = LinearEasing)
        )
    }

    Column {
        Text("${viewModel.currentRoundIndex}")
        AppContainer {
            BiDirectionalProgressBar(
                progress = animatableProgress.value,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "${currentMs / 1000} / ${durationMs / 1000}")
        }
    }
}

@Composable
private fun GuessingContent(viewModel: GameViewModel, state: GameUiState.Guessing) {
    Text("${state.currentRoundIndex}")

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

@Composable
private fun AnswerContent(viewModel: GameViewModel, state: GameUiState.Answer) {
    Column {
        Text("${state.currentRoundIndex}")
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
        AppButton(
            icon = painterResource(R.drawable.chevron_right),
            onClick = { viewModel.startRoundOrGameOver() },
            text = "Дальше"
        )
    }
}

@Composable
private fun GameOverContent(state: GameUiState.GameOver, onEnd: () -> Unit) {
    Column {
        Text(
            text = "Game Over",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Угадано: ${state.score}",
            style = MaterialTheme.typography.bodyLarge
        )

        AppButton(
            icon = painterResource(R.drawable.home),
            onClick = onEnd,
            text = "В главное меню"
        )
    }
}

@Composable
private fun ErrorContent(state: GameUiState.Error, onEnd: () -> Unit) {
    Column {
        ErrorContainer {
            Text(state.message)
        }
        AppButton(
            icon = painterResource(R.drawable.home),
            onClick = onEnd,
            text = "В главное меню"
        )
    }
}