package org.mromichov.guessthesong.presentation.game.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun QuestionCover(rotating: Boolean) {
    val angle = if (rotating) {
        val transition = rememberInfiniteTransition(label = "question cover")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(2_000, easing = LinearEasing), RepeatMode.Restart),
            label = "question rotation",
        )
        value
    } else 0f

    Box(
        modifier = Modifier
            .size(50.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Обложка скрыта" }
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .rotate(angle),
        contentAlignment = Alignment.Center,
    ) {
        Text("?", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.headlineMedium)
    }
}
