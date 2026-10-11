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
import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.data.remote.itunes.ItunesMapper

class TrackPreviewRepositoryImplTest {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
    }

    private fun createClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    @Test
    fun getPreview_whenItunesReturnsMatch_returnsMappedPreview() = runTest {
        val response = """
            {
              "resultCount": 1,
              "results": [
                {
                  "trackId": 555,
                  "artistName": "Queen",
                  "trackName": "Bohemian Rhapsody",
                  "previewUrl": "https://audio-ssl.itunes.apple.com/bohemian.m4a",
                  "artworkUrl100": "https://is1-ssl.mzstatic.com/queen.jpg"
                }
              ]
            }
        """.trimIndent()
        val engine = MockEngine {
            respond(
                content = response,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createClient(engine)

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper())

            val result = repository.getPreview("Queen", "Bohemian Rhapsody")

            assertTrue("Expected preview lookup to succeed", result.isSuccess)
            assertEquals("Queen", result.getOrThrow().artist)
            assertEquals("Bohemian Rhapsody", result.getOrThrow().title)
            assertEquals(
                "https://audio-ssl.itunes.apple.com/bohemian.m4a",
                result.getOrThrow().trackPreviewUrl,
            )
            assertEquals("https://is1-ssl.mzstatic.com/queen.jpg", result.getOrThrow().coverUrl)
            assertEquals(1, engine.requestHistory.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun getPreview_whenNoResultHasPreview_returnsFailure() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"resultCount":0,"results":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createClient(engine)

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper())

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
        val engine = MockEngine {
            throw IllegalStateException("simulated network failure")
        }
        val client = createClient(engine)

        try {
            val repository = TrackPreviewRepositoryImpl(ItunesApi(client), ItunesMapper())

            val result = repository.getPreview("Artist", "Track")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalStateException)
        } finally {
            client.close()
        }
    }
}
