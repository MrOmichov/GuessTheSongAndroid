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
import org.junit.Assert.assertNull
import org.junit.Test
import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.domain.model.TrackPreview

class ItunesTrackArtworkRepositoryTest {
    private fun preview(url: String) = TrackPreview(
        title = "Song",
        artist = "Artist",
        trackPreviewUrl = url,
        coverUrl = null,
    )

    private fun client(jsonBody: String, onRequest: () -> Unit = {}): HttpClient = HttpClient(
        MockEngine {
            onRequest()
            respond(
                jsonBody,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Test
    fun `uses artwork already attached to selected preview without an iTunes request`() = runTest {
        val http = HttpClient(MockEngine { error("iTunes must be skipped for a selected artwork URL") })
        try {
            val repository = ItunesTrackArtworkRepository(ItunesApi(http))

            assertEquals(
                "https://art.test/attached.jpg",
                repository.getCoverUrl(preview("https://audio.test/selected.m4a").copy(coverUrl = "https://art.test/attached.jpg")),
            )
        } finally {
            http.close()
        }
    }

    @Test
    fun `returns artwork only from result with the selected audio preview URL`() = runTest {
        val http = client(
            """{"resultCount":2,"results":[
                {"artistName":"Artist","trackName":"Song","previewUrl":"https://audio.test/other.m4a","artworkUrl100":"https://art.test/wrong.jpg"},
                {"artistName":"Artist","trackName":"Song","previewUrl":"https://audio.test/selected.m4a","artworkUrl100":"https://art.test/right.jpg"}
            ]}""",
        )
        try {
            val repository = ItunesTrackArtworkRepository(ItunesApi(http))

            assertEquals(
                "https://art.test/right.jpg",
                repository.getCoverUrl(preview("https://audio.test/selected.m4a")),
            )
        } finally {
            http.close()
        }
    }

    @Test
    fun `returns null if search has no matching preview URL`() = runTest {
        val http = client(
            """{"resultCount":1,"results":[{"previewUrl":"https://audio.test/different.m4a","artworkUrl100":"https://art.test/wrong.jpg"}]}""",
        )
        try {
            val repository = ItunesTrackArtworkRepository(ItunesApi(http))

            assertNull(repository.getCoverUrl(preview("https://audio.test/selected.m4a")))
        } finally {
            http.close()
        }
    }

    @Test
    fun `returns null for missing or blank artwork on matching preview`() = runTest {
        val bodies = listOf(
            """{"resultCount":1,"results":[{"previewUrl":"https://audio.test/selected.m4a"}]}""",
            """{"resultCount":1,"results":[{"previewUrl":"https://audio.test/selected.m4a","artworkUrl100":"  "}]}""",
        )
        for (body in bodies) {
            val http = client(body)
            try {
                val repository = ItunesTrackArtworkRepository(ItunesApi(http))

                assertNull(repository.getCoverUrl(preview("https://audio.test/selected.m4a")))
            } finally {
                http.close()
            }
        }
    }

    @Test
    fun `uses requested track identity in iTunes lookup`() = runTest {
        var requestUrl = ""
        val http = HttpClient(MockEngine { request ->
            requestUrl = request.url.toString()
            respond(
                """{"resultCount":0,"results":[]}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            val repository = ItunesTrackArtworkRepository(ItunesApi(http))
            repository.getCoverUrl(preview("https://audio.test/selected.m4a"))

            assert(requestUrl.contains("term=Artist%20Song") || requestUrl.contains("term=Artist+Song"))
            assert(requestUrl.contains("limit=5"))
        } finally {
            http.close()
        }
    }
}
