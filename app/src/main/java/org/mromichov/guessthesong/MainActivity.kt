package org.mromichov.guessthesong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelProvider
import org.mromichov.guessthesong.core.di.LocalViewModelProvider
import org.mromichov.guessthesong.core.navigation.AppNavigation
import org.mromichov.guessthesong.core.theme.GuessTheSongTheme
import javax.inject.Inject

class MainActivity : ComponentActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        (application as App).appComponent.inject(this)

        setContent {
            GuessTheSongTheme {
                CompositionLocalProvider(LocalViewModelProvider provides viewModelFactory) {
                    AppNavigation()
                }
            }
        }
    }
}