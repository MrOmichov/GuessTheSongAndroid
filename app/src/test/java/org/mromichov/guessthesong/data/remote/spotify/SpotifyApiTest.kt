package org.mromichov.guessthesong.data.remote.spotify

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SpotifyApiTest {

    @Test
    fun getPlaylist_embedHtml_extractsAndParsesJsonSuccessfully() = runTest {
        val mockHtml = """
        <!DOCTYPE html>
        <html>
        <head><title>Spotify Embed</title></head>
        <body>
        <script id="__NEXT_DATA__" type="application/json">
        {
            "props": {
                "pageProps": {
                    "state": {
                        "data": {
                            "entity": {
                                "id": "4lcPGoGgUtNpexPogG2ctm",
                                "name": "Dream Theater ballads",
                                "coverArt": {
                                    "sources": [
                                        { "url": "https://mosaic.scdn.co/640/cover.jpg", "height": 640, "width": 640 }
                                    ]
                                },
                                "trackList": [
                                    {
                                        "uri": "spotify:track:2vzfV6LgfAWtv7JeSoEPFO",
                                        "title": "The Silent Man",
                                        "subtitle": "Dream Theater",
                                        "duration": 227733
                                    }
                                ]
                            }
                        }
                    }
                }
            }
        }
        </script>
        </body>
        </html>
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertEquals("https://open.spotify.com/embed/playlist/4lcPGoGgUtNpexPogG2ctm", request.url.toString())
            respond(
                content = mockHtml,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8")
            )
        }

        val client = HttpClient(mockEngine)
        val api = SpotifyApi(client)

        val result = api.getPlaylist("4lcPGoGgUtNpexPogG2ctm")

        assertNotNull(result)
        val entity = result.props?.pageProps?.state?.data?.entity
        assertEquals("4lcPGoGgUtNpexPogG2ctm", entity?.id)
        assertEquals("Dream Theater ballads", entity?.name)
        assertEquals(1, entity?.trackList?.size)
        assertEquals("The Silent Man", entity?.trackList?.first()?.title)
        assertEquals("Dream Theater", entity?.trackList?.first()?.subtitle)
    }

    @Test(expected = NoSuchElementException::class)
    fun getPlaylist_invalidHtmlWithoutNextData_throwsNoSuchElementException() = runTest {
        val mockHtml = "<html><body><div>Empty response</div></body></html>"

        val mockEngine = MockEngine {
            respond(
                content = mockHtml,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8")
            )
        }

        val client = HttpClient(mockEngine)
        val api = SpotifyApi(client)

        api.getPlaylist("invalid_playlist_id")
    }
}
