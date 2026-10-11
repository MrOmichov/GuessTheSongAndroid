package org.mromichov.guessthesong.presentation.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.mromichov.guessthesong.R
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
                is GameUiState.ListeningPreview -> {
                    ListeningPreviewContent(viewModel)
                }

                is GameUiState.Guessing -> {
                    val state = uiState as GameUiState.Guessing
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

                is GameUiState.Answer -> {
                    val state = uiState as GameUiState.Answer
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

                is GameUiState.GameOver -> {
                    val state = uiState as GameUiState.GameOver
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

                is GameUiState.Error -> {
                    val state = uiState as GameUiState.Error
                    ErrorContainer {
                        Text(state.message)
                    }
                }
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
                trackColor = MaterialTheme.colorScheme.primaryContainer,
            )
            Text(text = "${currentMs / 1000} / ${durationMs / 1000}")
        }
    }
}

@Composable
fun BiDirectionalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Blue,
    trackColor: Color = Color.LightGray,
    strokeWidth: Dp = 8.dp
) {
    val coercedProgress = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val strokeWidthPx = strokeWidth.toPx()

        drawLine(
            color = trackColor,
            start = Offset(x = 0f, y = centerY),
            end = Offset(x = width, y = centerY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round
        )

        val centerX = width / 2
        val halfFilledWidth = centerX * coercedProgress
        val startX = centerX - halfFilledWidth
        val endX = centerX + halfFilledWidth

        if (coercedProgress > 0f) {
            drawLine(
                color = color,
                start = Offset(x = startX, y = centerY),
                end = Offset(x = endX, y = centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}
