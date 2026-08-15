package org.mromichov.guessthesong.data.remote.itunes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.core.network.HttpClientCreator
import org.mromichov.guessthesong.data.repository.TrackPreviewRepositoryImpl

class ItunesLiveIntegrationTest {

    private val realClient = HttpClientCreator.create()
    private val api = ItunesApi(realClient)
    private val mapper = ItunesMapper()
    private val repository = TrackPreviewRepositoryImpl(api, mapper)

    @Test
    fun fetchLive_itunesTrackPreview_returnsValidM4aUrl() = runTest {
        val artist = "Linkin Park"
        val title = "In the End"

        val result = repository.getPreview(artist = artist, title = title)

        assertTrue("Expected successful preview lookup, got: ${result.exceptionOrNull()}", result.isSuccess)
        val preview = result.getOrThrow()

        println("=== LIVE ITUNES PREVIEW ===")
        println("Artist: ${preview.artistName}")
        println("Track: ${preview.trackName}")
        println("Preview URL: ${preview.previewUrl}")
        println("Artwork URL: ${preview.artworkUrl}")

        assertTrue("Artist name should not be blank", preview.artistName.isNotBlank())
        assertTrue("Track name should not be blank", preview.trackName.isNotBlank())
        assertTrue("Preview URL should not be blank", preview.previewUrl.isNotBlank())
        assertTrue("Preview URL should start with https://", preview.previewUrl.startsWith("https://"))
        assertTrue("Preview URL should contain .m4a or .aac or .mp4", preview.previewUrl.contains(".m4a") || preview.previewUrl.contains(".aac") || preview.previewUrl.contains(".mp4"))
    }
}
