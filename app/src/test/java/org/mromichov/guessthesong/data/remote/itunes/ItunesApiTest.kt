package org.mromichov.guessthesong.data.remote.itunes

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
import org.junit.Assert.assertNotNull
import org.junit.Test

class ItunesApiTest {

    private val jsonConfig = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    @Test
    fun searchTrackPreview_success() = runTest {
        val mockJson = """
        {
            "resultCount": 1,
            "results": [
                {
                    "trackId": 123456789,
                    "artistName": "Linkin Park",
                    "trackName": "In the End",
                    "previewUrl": "https://audio-ssl.itunes.apple.com/preview.m4a",
                    "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/cover.jpg/100x100bb.jpg"
                }
            ]
        }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            val urlString = request.url.toString()
            assert(urlString.contains("itunes.apple.com/search"))
            assert(urlString.contains("media=music"))
            assert(urlString.contains("entity=song"))
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
        }

        val api = ItunesApi(httpClient)
        val response = api.searchTrackPreview("Linkin Park In the End")

        assertEquals(1, response.resultCount)
        assertEquals(1, response.results.size)
        val track = response.results.first()
        assertEquals(123456789L, track.trackId)
        assertEquals("Linkin Park", track.artistName)
        assertEquals("In the End", track.trackName)
        assertEquals("https://audio-ssl.itunes.apple.com/preview.m4a", track.previewUrl)
        assertNotNull(track.artworkUrl100)
    }
}
