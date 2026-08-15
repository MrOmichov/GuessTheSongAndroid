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
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    private fun createRepository(mockEngine: MockEngine): TrackPreviewRepositoryImpl {
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
        }
        val api = ItunesApi(client)
        val mapper = ItunesMapper()
        return TrackPreviewRepositoryImpl(api, mapper)
    }

    @Test
    fun getPreview_success() = runTest {
        val mockJson = """
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

        val mockEngine = MockEngine {
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repo = createRepository(mockEngine)
        val result = repo.getPreview("Queen", "Bohemian Rhapsody")

        assertTrue(result.isSuccess)
        val preview = result.getOrNull()
        assertEquals("Queen", preview?.artistName)
        assertEquals("Bohemian Rhapsody", preview?.trackName)
        assertEquals("https://audio-ssl.itunes.apple.com/bohemian.m4a", preview?.previewUrl)
    }

    @Test
    fun getPreview_notFound_returnsFailure() = runTest {
        val mockJson = """
        {
            "resultCount": 0,
            "results": []
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repo = createRepository(mockEngine)
        val result = repo.getPreview("UnknownArtist", "NonExistentTrack")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }
}
