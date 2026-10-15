package org.mromichov.guessthesong.presentation.playlists

import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
import java.io.File

class PlaylistCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cardShowsOfflineCoverNodeAndProviderAndUpdatesProviderAfterReplacement() {
        val playlist = mutableStateOf(
            LocalPlaylist(
                id = 1L,
                content = PlaylistContent("Favorites", listOf(Track("Song", "Artist", null, 0L))),
                coverFileName = "favorites.jpg",
                provider = PlaylistProvider.SPOTIFY,
            ),
        )
        composeRule.setContent { MaterialTheme { PlaylistCard(playlist.value) } }

        composeRule.onNodeWithText("Favorites").assertExists()
        composeRule.onNodeWithContentDescription("Обложка плейлиста Favorites").assertExists()
        composeRule.onNodeWithContentDescription("Spotify").assertExists()

        composeRule.runOnIdle {
            playlist.value = playlist.value.copy(provider = PlaylistProvider.YANDEX_MUSIC)
        }
        composeRule.onNodeWithContentDescription("Яндекс Музыка").assertExists()
        composeRule.onNodeWithContentDescription("Spotify").assertDoesNotExist()

        composeRule.runOnIdle {
            playlist.value = playlist.value.copy(provider = null)
        }
        composeRule.onNodeWithContentDescription("Яндекс Музыка").assertDoesNotExist()
    }

    @Test
    fun cardKeepsCoverAndProviderSemanticsWhenLocalFileIsMissing() {
        composeRule.setContent { MaterialTheme {
            PlaylistCard(
                LocalPlaylist(
                    id = 2L,
                    content = PlaylistContent("Offline", emptyList()),
                    coverFileName = "missing-cover.jpg",
                    provider = PlaylistProvider.YANDEX_MUSIC,
                ),
            )
        } }

        composeRule.onNodeWithContentDescription("Обложка плейлиста Offline").assertExists()
        composeRule.onNodeWithContentDescription("Яндекс Музыка").assertExists()
    }

    @Test
    fun longPlaylistTitleKeepsProviderIconInsideCardBounds() {
        val title = "A playlist title that is long enough to wrap across two lines"
        composeRule.setContent {
            MaterialTheme {
                PlaylistCard(
                    LocalPlaylist(
                        id = 6L,
                        content = PlaylistContent(title, emptyList()),
                        provider = PlaylistProvider.SPOTIFY,
                    ),
                )
            }
        }

        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val provider = composeRule.onNodeWithContentDescription("Spotify").getUnclippedBoundsInRoot()
        composeRule.onNodeWithText(title).assertExists()
        assertTrue(provider.right <= root.right)
        assertTrue(provider.bottom <= root.bottom)
        assertTrue(provider.left >= root.left)
        assertTrue(provider.top >= root.top)
    }

    @Test
    fun cardRendersValidOfflinePngAndFallsBackForCorruptOrMissingFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val coversDirectory = File(
            context.filesDir,
            "playlist-covers",
        ).apply { mkdirs() }
        val validFile = File(coversDirectory, "compose-test.png")
        val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.GREEN)
        validFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        File(coversDirectory, "corrupt.img").writeText("not an image")
        val playlist = mutableStateOf(
            LocalPlaylist(
                id = 3L,
                content = PlaylistContent("Rendered cover", emptyList()),
                coverFileName = validFile.name,
            ),
        )

        try {
            composeRule.setContent { MaterialTheme { PlaylistCard(playlist.value) } }

            composeRule.waitUntil(5_000) {
                val pixels = composeRule.onNodeWithContentDescription(
                    "Обложка плейлиста Rendered cover",
                ).captureToImage().toPixelMap()
                val center = pixels[pixels.width / 2, pixels.height / 2]
                center.green > 0.9f && center.red < 0.1f
            }
            val renderedCard = composeRule.onRoot().captureToImage().asAndroidBitmap()
            File(context.filesDir, "playlist-card-test.png").outputStream().use {
                renderedCard.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            renderedCard.recycle()

            composeRule.runOnIdle {
                playlist.value = LocalPlaylist(
                    id = 4L,
                    content = PlaylistContent("Corrupt cover", emptyList()),
                    coverFileName = "corrupt.img",
                )
            }
            composeRule.waitUntil(5_000) {
                val pixels = composeRule.onNodeWithContentDescription(
                    "Обложка плейлиста Corrupt cover",
                ).captureToImage().toPixelMap()
                pixels[pixels.width / 2, pixels.height / 2].green < 0.9f
            }
            composeRule.onNodeWithContentDescription("Обложка плейлиста Corrupt cover").assertExists()

            composeRule.runOnIdle {
                playlist.value = LocalPlaylist(5L, PlaylistContent("No cover", emptyList()))
            }
            composeRule.onNodeWithContentDescription("Обложка плейлиста No cover").assertExists()
        } finally {
            validFile.delete()
            File(coversDirectory, "corrupt.img").delete()
        }
    }
}
