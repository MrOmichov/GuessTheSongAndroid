package org.mromichov.guessthesong.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.core.database.entity.TrackEntity
import org.mromichov.guessthesong.data.dao.FakePlaylistTracksDao
import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.data.remote.itunes.ItunesMapper

class TrackPreviewRepositoryImplTest {

    private fun createClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    @Test
    fun getPreview_whenItunesReturnsMatch_returnsMappedPreview() = runTest {
        val response = """
            {"resultCount":1,"results":[{"trackId":555,"artistName":"Queen",
              "trackName":"Bohemian Rhapsody","previewUrl":"https://audio-ssl.itunes.apple.com/bohemian.m4a",
              "artworkUrl100":"https://is1-ssl.mzstatic.com/queen.jpg"}]}
        """.trimIndent()
        val engine = MockEngine {
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createClient(engine)

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper(), FakePlaylistTracksDao())
            val result = repository.getPreview("Queen", "Bohemian Rhapsody")

            assertTrue("Expected preview lookup to succeed", result.isSuccess)
            assertEquals("Queen", result.getOrThrow().artist)
            assertEquals("Bohemian Rhapsody", result.getOrThrow().title)
            assertEquals("https://audio-ssl.itunes.apple.com/bohemian.m4a", result.getOrThrow().trackPreviewUrl)
            assertEquals("https://is1-ssl.mzstatic.com/queen.jpg", result.getOrThrow().coverUrl)
            assertEquals(1, engine.requestHistory.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun getPreview_whenNoResultHasPreview_returnsFailure() = runTest {
        val engine = MockEngine {
            respond("""{"resultCount":0,"results":[]}""", HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createClient(engine)

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper(), FakePlaylistTracksDao())
            val result = repository.getPreview("Unknown Artist", "Unknown Track")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NoSuchElementException)
            assertEquals(1, engine.requestHistory.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun getPreview_whenNetworkRequestFails_returnsFailure() = runTest {
        val engine = MockEngine { throw IllegalStateException("simulated network failure") }
        val client = createClient(engine)

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper(), FakePlaylistTracksDao())
            val result = repository.getPreview("Artist", "Track")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
        } finally {
            client.close()
        }
    }

    @Test
    fun getPreview_whenDatabaseHasUrl_returnsCachedPreviewWithoutItunesRequest() = runTest {
        var requestCount = 0
        val engine = MockEngine {
            requestCount++
            error("iTunes should not be requested for a cache hit")
        }
        val client = createClient(engine)
        val dao = FakePlaylistTracksDao().apply {
            seedTrack(TrackEntity("Cached song", "Cached artist", "https://cached.test/song.m4a"))
        }

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper(), dao)
            val result = repository.getPreview("Cached artist", "Cached song")

            assertTrue(result.isSuccess)
            assertEquals("Cached song", result.getOrThrow().title)
            assertEquals("Cached artist", result.getOrThrow().artist)
            assertEquals("https://cached.test/song.m4a", result.getOrThrow().trackPreviewUrl)
            assertEquals(0, requestCount)
        } finally {
            client.close()
        }
    }

    @Test
    fun getPreview_whenDatabaseUrlMissing_fetchesAndCachesUsingRequestedIdentity() = runTest {
        val response = """
            {"resultCount":1,"results":[{"trackId":555,"artistName":"Queen Official",
              "trackName":"Bohemian Rhapsody (Remastered)","previewUrl":"https://audio.test/found.m4a",
              "artworkUrl100":"https://art.test/cover.jpg"}]}
        """.trimIndent()
        var requestCount = 0
        val engine = MockEngine {
            requestCount++
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createClient(engine)
        val dao = FakePlaylistTracksDao().apply {
            seedTrack(TrackEntity("Bohemian Rhapsody", "Queen", null))
        }

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper(), dao)
            val result = repository.getPreview("Queen", "Bohemian Rhapsody")

            assertTrue(result.isSuccess)
            assertEquals("https://audio.test/found.m4a", result.getOrThrow().trackPreviewUrl)
            assertEquals("https://audio.test/found.m4a", dao.findTrackPreviewUrlByTitleAndArtist("Bohemian Rhapsody", "Queen"))
            assertEquals(1, requestCount)
        } finally {
            client.close()
        }
    }

    @Test
    fun getPreview_whenCacheWriteFails_stillReturnsFetchedPreview() = runTest {
        val response = """
            {"resultCount":1,"results":[{"trackId":555,"artistName":"Artist",
              "trackName":"Track","previewUrl":"https://audio.test/track.m4a"}]}
        """.trimIndent()
        val engine = MockEngine {
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = createClient(engine)
        val dao = FakePlaylistTracksDao().apply {
            seedTrack(TrackEntity("Track", "Artist", null))
            failPreviewUpdate = true
        }

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper(), dao)
            val result = repository.getPreview("Artist", "Track")

            assertTrue("Cache persistence failure should not discard a fetched preview", result.isSuccess)
            assertEquals("https://audio.test/track.m4a", result.getOrThrow().trackPreviewUrl)
        } finally {
            client.close()
        }
    }
}
