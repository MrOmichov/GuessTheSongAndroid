package org.mromichov.guessthesong.data.remote.itunes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.core.network.HttpClientCreator
import org.mromichov.guessthesong.data.dao.FakeTrackDao
import org.mromichov.guessthesong.data.repository.TrackPreviewRepositoryImpl

class ItunesLiveIntegrationTest {

    private val realClient = HttpClientCreator.create()
    private val api = ItunesApi(realClient)
    private val mapper = ItunesMapper()
    private val fakeDao = FakeTrackDao()
    private val repository = TrackPreviewRepositoryImpl(fakeDao, api, mapper)

    @Test
    fun fetchLive_itunesTrackPreview_returnsValidM4aUrl() = runTest {
        val artist = "Linkin Park"
        val title = "In the End"

        val result = repository.getPreview(artist = artist, title = title)

        assertTrue("Expected successful preview lookup, got: ${result.exceptionOrNull()}", result.isSuccess)
        val preview = result.getOrThrow()

        println("=== LIVE ITUNES PREVIEW ===")
        println("Artist: ${preview.artist}")
        println("Track: ${preview.title}")
        println("Preview URL: ${preview.trackPreviewUrl}")
        println("Artwork URL: ${preview.coverUrl}")

        assertTrue("Artist name should not be blank", preview.artist.isNotBlank())
        assertTrue("Track name should not be blank", preview.title.isNotBlank())
        assertTrue("Preview URL should not be blank", preview.trackPreviewUrl.isNotBlank())
        assertTrue("Preview URL should start with https://", preview.trackPreviewUrl.startsWith("https://"))
        assertTrue("Preview URL should contain .m4a or .aac or .mp4", preview.trackPreviewUrl.contains(".m4a") || preview.trackPreviewUrl.contains(".aac") || preview.trackPreviewUrl.contains(".mp4"))
    }
}
