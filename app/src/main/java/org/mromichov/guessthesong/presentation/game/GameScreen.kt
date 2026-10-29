package org.mromichov.guessthesong.presentation.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.asPainter
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.presentation.game.artwork.CoilPreparedCover
import org.mromichov.guessthesong.presentation.game.component.BiDirectionalProgressBar
import org.mromichov.guessthesong.presentation.game.component.QuestionCover
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
            animationSpec = tween(durationMillis = 16, easing = LinearEasing)
        )
    }

    Column {
        Text("${viewModel.currentRoundIndex + 1}") // TODO не делать +1 а передавать через State
        AppContainer {
            Column {
                QuestionCover(rotating = true)
                BiDirectionalProgressBar(
                    progress = animatableProgress.value,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = "${currentMs / 1000} / ${durationMs / 1000}")
            }
        }
    }
}

@Composable
private fun GuessingContent(viewModel: GameViewModel, state: GameUiState.Guessing) {
    Column {
        Text("${state.currentRoundIndex + 1}")
        AppContainer { QuestionCover(rotating = false) }
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
}

@Composable
private fun AnswerContent(viewModel: GameViewModel, state: GameUiState.Answer) {
    val preparedCoverImage = (state.preparedCover as? CoilPreparedCover)?.image
    val coverPainter = preparedCoverImage?.asPainter(context = LocalContext.current)
    Column {
        Text("${state.currentRoundIndex + 1}")
        AppContainer {
            Column {
                Image(
                    painter = coverPainter ?: painterResource(R.drawable.app_icon_playstore),
                    contentDescription = "Обложка трека",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                )
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
