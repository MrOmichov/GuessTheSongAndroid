package org.mromichov.guessthesong.core.di

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelProvider

val LocalViewModelProvider = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("No ViewModelFactory provided")
}