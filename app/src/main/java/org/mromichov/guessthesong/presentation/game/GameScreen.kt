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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.asPainter
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.presentation.error.resolve
import org.mromichov.guessthesong.presentation.game.artwork.CoilPreparedCover
import org.mromichov.guessthesong.presentation.game.component.BiDirectionalProgressBar
import org.mromichov.guessthesong.presentation.game.component.QuestionCover
import org.mromichov.guessthesong.presentation.sharedcomponent.AppButton
import org.mromichov.guessthesong.presentation.sharedcomponent.AppContainer
import org.mromichov.guessthesong.presentation.sharedcomponent.ErrorContainer

// TODO refactor
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
        Text((viewModel.currentRoundIndex + 1).toString()) // TODO не делать +1 а передавать через State
        AppContainer {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                QuestionCover(rotating = true)
                BiDirectionalProgressBar(
                    progress = animatableProgress.value,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = stringResource(R.string.duration_progress, currentMs / 1000, durationMs / 1000))
            }
        }
    }
}

@Composable
private fun GuessingContent(viewModel: GameViewModel, state: GameUiState.Guessing) {
    Column {
        Text((state.currentRoundIndex + 1).toString())
        AppContainer {
            Box(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                QuestionCover(rotating = false)
            }
        }
        LazyColumn {
            items(state.options.size) {
                OutlinedButton(
                    onClick = { viewModel.checkAnswer(state.options[it]) }
                ) {
                    Text(
                        text = stringResource(
                            R.string.track_option,
                            state.options[it].title,
                            state.options[it].artist
                        )
                    )
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
        Text((state.currentRoundIndex + 1).toString())
        AppContainer {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = coverPainter ?: painterResource(R.drawable.app_icon_playstore),
                    contentDescription = stringResource(R.string.track_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                )
                Text(stringResource(R.string.correct_answer, state.answer.title, state.answer.artist))
                Text(
                    if (state.isGuessed)
                        stringResource(R.string.answer_correct)
                    else
                        stringResource(R.string.answer_incorrect)
                )
            }
        }
        AppButton(
            icon = painterResource(R.drawable.chevron_right),
            onClick = { viewModel.startRoundOrGameOver() },
            text = stringResource(R.string.next)
        )
    }
}

@Composable
private fun GameOverContent(state: GameUiState.GameOver, onEnd: () -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.game_over),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.score_guessed, state.score),
            style = MaterialTheme.typography.bodyLarge
        )

        AppButton(
            icon = painterResource(R.drawable.home),
            onClick = onEnd,
            text = stringResource(R.string.main_menu)
        )
    }
}

@Composable
private fun ErrorContent(state: GameUiState.Error, onEnd: () -> Unit) {
    Column {
        ErrorContainer {
            Text(state.message.resolve())
        }
        AppButton(
            icon = painterResource(R.drawable.home),
            onClick = onEnd,
            text = stringResource(R.string.main_menu)
        )
    }
}
