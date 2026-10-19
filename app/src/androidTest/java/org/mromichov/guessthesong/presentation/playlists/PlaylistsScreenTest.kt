package org.mromichov.guessthesong.presentation.playlists

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
import org.mromichov.guessthesong.domain.model.playlist.RemotePlaylist
import org.mromichov.guessthesong.domain.repository.PlaylistRepository

@RunWith(AndroidJUnit4::class)
class PlaylistsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sameNamedPlaylistCardsRemainClickableAndReportTheirOwnIds() {
        val repository = FakePlaylistRepository(
            playlists = {
                Result.success(
                    listOf(
                        localPlaylist(101L, "Duplicate name"),
                        localPlaylist(202L, "Duplicate name"),
                    ),
                )
            },
        )
        val viewModel = PlaylistsViewModel(repository)
        val selectedIds = mutableListOf<Long>()
        showScreen(viewModel, onSelect = selectedIds::add)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Duplicate name").fetchSemanticsNodes().size == 2
        }

        composeRule.onAllNodesWithText("Duplicate name")[0].performClick()
        composeRule.runOnIdle { assertEquals(listOf(101L), selectedIds) }
        composeRule.onAllNodesWithText("Duplicate name")[1].performClick()
        composeRule.runOnIdle { assertEquals(listOf(101L, 202L), selectedIds) }
    }

    @Test
    fun emptyListShowsImportActionAndInvokesItsCallback() {
        val repository = FakePlaylistRepository(playlists = { Result.success(emptyList()) })
        val viewModel = PlaylistsViewModel(repository)
        var importCalls = 0
        showScreen(viewModel, onEmptyImport = { importCalls++ })

        composeRule.onNodeWithText("Сохранённых плейлистов пока нет").assertIsDisplayed()
        composeRule.onNodeWithText("Загрузить плейлист").performClick()

        composeRule.runOnIdle { assertEquals(1, importCalls) }
        composeRule.onNodeWithText("Перезагрузить").assertDoesNotExist()
    }

    @Test
    fun retryAfterLoadErrorRequestsPlaylistsAgainAndShowsResult() {
        val expected = localPlaylist(44L, "Recovered playlist")
        var calls = 0
        val repository = FakePlaylistRepository(
            playlists = {
                calls++
                if (calls == 1) Result.failure(IllegalStateException("storage unavailable"))
                else Result.success(listOf(expected))
            },
        )
        val viewModel = PlaylistsViewModel(repository)
        showScreen(viewModel)

        composeRule.onNodeWithText("storage unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Перезагрузить").performClick()
        composeRule.onNodeWithText("Recovered playlist").assertIsDisplayed()

        composeRule.runOnIdle { assertEquals(2, calls) }
    }

    private fun showScreen(
        viewModel: PlaylistsViewModel,
        onSelect: (Long) -> Unit = {},
        onEmptyImport: () -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                PlaylistsScreen(
                    viewModel = viewModel,
                    back = {},
                    onPlaylistSelected = onSelect,
                    onEmptyListImport = onEmptyImport,
                )
            }
        }
    }

    private fun localPlaylist(id: Long, title: String) = LocalPlaylist(
        id = id,
        content = PlaylistContent(title, emptyList()),
    )

    private class FakePlaylistRepository(
        private val playlists: suspend () -> Result<List<LocalPlaylist>> = { Result.success(emptyList()) },
    ) : PlaylistRepository {
        override suspend fun fetchPlaylistByUrl(url: String): Result<RemotePlaylist> =
            Result.failure(UnsupportedOperationException("Not used in PlaylistsScreenTest"))

        override suspend fun getLocalPlaylists(): Result<List<LocalPlaylist>> = playlists()

        override suspend fun getLocalPlaylist(id: Long): Result<LocalPlaylist> =
            Result.failure(NoSuchElementException("Playlist $id not found"))

        override suspend fun findLocalPlaylistByTitle(title: String): Result<LocalPlaylist?> =
            Result.success(null)

        override suspend fun savePlaylist(
            playlistContent: PlaylistContent,
            provider: PlaylistProvider?,
        ): Result<LocalPlaylist> = Result.success(LocalPlaylist(1L, playlistContent, provider = provider))

        override suspend fun replacePlaylist(
            existingId: Long,
            playlistContent: PlaylistContent,
            provider: PlaylistProvider?,
        ): Result<LocalPlaylist> =
            Result.success(LocalPlaylist(existingId, playlistContent, provider = provider))

        override suspend fun deletePlaylist(id: Long): Result<Unit> = Result.success(Unit)
    }
}
