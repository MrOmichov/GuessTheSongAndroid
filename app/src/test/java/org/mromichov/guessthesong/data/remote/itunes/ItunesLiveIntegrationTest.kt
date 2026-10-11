package org.mromichov.guessthesong.data.remote.itunes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.mromichov.guessthesong.core.network.HttpClientCreator
import org.mromichov.guessthesong.data.dao.FakePlaylistTracksDao
import org.mromichov.guessthesong.data.repository.TrackPreviewRepositoryImpl

class ItunesLiveIntegrationTest {

    @Test
    fun fetchLive_itunesTrackPreview_returnsValidM4aUrl() = runTest {
        assumeTrue(
            "Set RUN_ITUNES_LIVE_TESTS=true to enable live iTunes integration tests",
            System.getenv("RUN_ITUNES_LIVE_TESTS") == "true",
        )

        val client = HttpClientCreator.create()
        try {
            val api = ItunesApi(client)
            val repository = TrackPreviewRepositoryImpl(api, ItunesMapper(), FakePlaylistTracksDao())
            val result = repository.getPreview(artist = "Linkin Park", title = "In the End")

            assertTrue("Expected successful preview lookup, got: ${result.exceptionOrNull()}", result.isSuccess)
            val preview = result.getOrThrow()
            assertTrue("Artist name should not be blank", preview.artist.isNotBlank())
            assertTrue("Track name should not be blank", preview.title.isNotBlank())
            assertTrue("Preview URL should not be blank", preview.trackPreviewUrl.isNotBlank())
            assertTrue("Preview URL should start with https://", preview.trackPreviewUrl.startsWith("https://"))
            assertTrue(
                "Preview URL should contain an audio extension",
                listOf(".m4a", ".aac", ".mp4").any { extension ->
                    preview.trackPreviewUrl.contains(extension)
                },
            )
        } finally {
            client.close()
        }
    }
}
