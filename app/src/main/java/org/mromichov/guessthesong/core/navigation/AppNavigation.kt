package org.mromichov.guessthesong.core.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.mromichov.guessthesong.core.di.LocalViewModelProvider
import org.mromichov.guessthesong.presentation.game.GameScreen
import org.mromichov.guessthesong.presentation.game.GameViewModel
import org.mromichov.guessthesong.presentation.settings.SettingsScreen
import org.mromichov.guessthesong.presentation.settings.SettingsViewModel
import org.mromichov.guessthesong.presentation.startinput.StartInputScreen
import org.mromichov.guessthesong.presentation.startinput.StartInputViewModel

// Routes
@Serializable
object StartInputRoute

@Serializable
object GameRoute

@Serializable
object SettingsRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = StartInputRoute) {
        composable<StartInputRoute> {
            val startInputViewModel: StartInputViewModel = viewModel(factory = LocalViewModelProvider.current)
            StartInputScreen(
                viewModel = startInputViewModel,
                onStartGame = { navController.navigate(GameRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) }
            )
        }

        composable<GameRoute> {
            val gameViewModel: GameViewModel = viewModel(factory = LocalViewModelProvider.current)
            GameScreen(viewModel = gameViewModel, onEnd = { navController.navigate(StartInputRoute) })
        }

        composable<SettingsRoute> {
            val settingsViewModel: SettingsViewModel = viewModel(factory = LocalViewModelProvider.current)
            SettingsScreen(
                viewModel = settingsViewModel,
                back = { navController.popBackStack() }
            )
        }
    }
}