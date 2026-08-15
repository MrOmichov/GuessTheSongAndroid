package org.mromichov.guessthesong.data.remote.yandex

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

class YandexApiTest {

    private val jsonConfig = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    @Test
    fun getUserPlaylist_success() = runTest {
        val mockJson = """
        {
            "result": {
                "title": "Чарт Яндекс Музыки",
                "tracks": [
                    {
                        "track": {
                            "id": "12345",
                            "title": "Mock Song Title",
                            "artists": [
                                { "id": 1, "name": "Artist One" },
                                { "id": 2, "name": "Artist Two" }
                            ],
                            "coverUri": "avatars.yandex.net/get-music-content/12345/%%",
                            "durationMs": 180000
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("https://api.music.yandex.net/users/yamusic-top/playlists/1000", request.url.toString())
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

        val api = YandexApi(httpClient)
        val response = api.getUserPlaylist(owner = "yamusic-top", kind = "1000")

        assertNotNull(response.result)
        assertEquals("Чарт Яндекс Музыки", response.result?.title)
        assertEquals(1, response.result?.tracks?.size)
        val track = response.result?.tracks?.first()?.track
        assertEquals("12345", track?.id)
        assertEquals("Mock Song Title", track?.title)
        assertEquals(2, track?.artists?.size)
        assertEquals("Artist One", track?.artists?.first()?.name)
    }

    @Test
    fun getUuidPlaylist_success() = runTest {
        val mockJson = """
        {
            "result": {
                "title": "Editorial Playlist",
                "tracks": [
                    {
                        "id": 999,
                        "track": {
                            "id": "67890",
                            "title": "UUID Song",
                            "artists": [
                                { "id": 3, "name": "Single Artist" }
                            ],
                            "ogImage": "avatars.yandex.net/get-music-content/67890/%%",
                            "durationMs": 210000
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("https://api.music.yandex.net/playlist/test-uuid-1234", request.url.toString())
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

        val api = YandexApi(httpClient)
        val response = api.getUuidPlaylist(uuid = "test-uuid-1234")

        assertNotNull(response.result)
        assertEquals("Editorial Playlist", response.result?.title)
        assertEquals(1, response.result?.tracks?.size)
        val track = response.result?.tracks?.first()?.track
        assertEquals("67890", track?.id)
        assertEquals("UUID Song", track?.title)
        assertEquals(1, track?.artists?.size)
        assertEquals("Single Artist", track?.artists?.first()?.name)
    }
}
