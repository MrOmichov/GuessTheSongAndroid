package org.mromichov.guessthesong.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.mromichov.guessthesong.core.di.LocalViewModelProvider
import org.mromichov.guessthesong.presentation.game.GameScreen
import org.mromichov.guessthesong.presentation.game.GameViewModel
import org.mromichov.guessthesong.presentation.playlists.PlaylistsScreen
import org.mromichov.guessthesong.presentation.playlists.PlaylistsViewModel
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

@Serializable
object PlaylistsRoute

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val viewModelFactory = LocalViewModelProvider.current

    NavHost(navController = navController, startDestination = StartInputRoute) {
        composable<StartInputRoute> { startEntry ->
            val startInputViewModel: StartInputViewModel = viewModel(
                viewModelStoreOwner = startEntry,
                factory = viewModelFactory,
            )
            StartInputScreen(
                viewModel = startInputViewModel,
                startGame = {
                    val isStartRouteActive = navController.currentDestination
                        ?.hasRoute<StartInputRoute>() == true
                    if (isStartRouteActive && startInputViewModel.consumeReadyToPlay()) {
                        navController.navigate(GameRoute) { launchSingleTop = true }
                    }
                },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenPlaylists = {
                    if (navController.currentDestination?.hasRoute<StartInputRoute>() == true) {
                        navController.navigate(PlaylistsRoute)
                    }
                },
            )
        }

        composable<GameRoute> {
            val gameViewModel: GameViewModel = viewModel(factory = viewModelFactory)
            GameScreen(viewModel = gameViewModel, onEnd = { navController.popBackStack() })
        }

        composable<SettingsRoute> {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = settingsViewModel,
                back = { navController.popBackStack() },
            )
        }

        composable<PlaylistsRoute> { playlistsEntry ->
            val playlistsViewModel: PlaylistsViewModel = viewModel(
                viewModelStoreOwner = playlistsEntry,
                factory = viewModelFactory,
            )
            val startInputEntry = remember(navController) {
                navController.getBackStackEntry<StartInputRoute>()
            }
            val startInputViewModel: StartInputViewModel = viewModel(
                viewModelStoreOwner = startInputEntry,
                factory = viewModelFactory,
            )
            var selectionHandled by remember(playlistsEntry.id) { mutableStateOf(false) }

            PlaylistsScreen(
                viewModel = playlistsViewModel,
                back = { navController.popBackStack() },
                onPlaylistSelected = { id ->
                    val isPlaylistsRouteActive = navController.currentDestination
                        ?.hasRoute<PlaylistsRoute>() == true
                    if (isPlaylistsRouteActive && !selectionHandled) {
                        selectionHandled = true
                        startInputViewModel.selectSavedPlaylist(id)
                        navController.popBackStack()
                    }
                },
                onEmptyListImport = {
                    val isPlaylistsRouteActive = navController.currentDestination
                        ?.hasRoute<PlaylistsRoute>() == true
                    if (isPlaylistsRouteActive && navController.popBackStack()) {
                        startInputViewModel.openImportForm()
                    }
                },
            )
        }
    }
}
