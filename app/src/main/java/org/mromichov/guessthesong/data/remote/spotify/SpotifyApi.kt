package org.mromichov.guessthesong.data.remote.spotify

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyWebEmbedDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyApi @Inject constructor(
    private val client: HttpClient,
) {
    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        private val NEXT_DATA_REGEX =
            Regex("""<script\s+id="__NEXT_DATA__"\s+type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    suspend fun getPlaylist(playlistId: String): SpotifyWebEmbedDto {
        val html = client.get("https://open.spotify.com/embed/playlist/$playlistId") {
            header(HttpHeaders.UserAgent, USER_AGENT)
        }.bodyAsText()

        val jsonString = NEXT_DATA_REGEX.find(html)?.groupValues?.get(1)
            ?: throw NoSuchElementException("Could not find __NEXT_DATA__ in Spotify embed HTML for playlist $playlistId")

        return json.decodeFromString<SpotifyWebEmbedDto>(jsonString)
    }
}
