package org.mromichov.guessthesong.presentation.game.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BiDirectionalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Blue,
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

        val centerX = width / 2
        val maxHalfWidth = (width - strokeWidthPx) / 2
        val halfFilledWidth = maxHalfWidth * coercedProgress

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