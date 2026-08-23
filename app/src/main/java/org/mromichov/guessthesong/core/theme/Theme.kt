package org.mromichov.guessthesong.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color.Black,
    primaryContainer = MintPrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = CyanSecondary,
    onSecondary = Color.Black,
    tertiary = AmberTertiary,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = AnswerWrong,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Color.Black,
    primaryContainer = MintPrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = CyanSecondary,
    onSecondary = Color.Black,
    tertiary = AmberTertiary,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = AnswerWrong,
    onError = Color.White
)

@Composable
fun GuessTheSongTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}