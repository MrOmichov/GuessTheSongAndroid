package org.mromichov.guessthesong.data.remote.yandex

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.mromichov.guessthesong.core.network.HttpClientCreator
import org.mromichov.guessthesong.data.dao.FakePlaylistTracksDao
import org.mromichov.guessthesong.data.local.NoOpPlaylistCoverStorage
import org.mromichov.guessthesong.data.repository.PlaylistRepositoryImpl

class YandexLiveIntegrationTest {

    @Test
    fun fetchLive_yandexTopChartPlaylist_returnsRealTracks() = runTest {
        assumeTrue(
            "Set RUN_YANDEX_LIVE_TESTS=true to enable live Yandex integration tests",
            System.getenv("RUN_YANDEX_LIVE_TESTS") == "true",
        )

        val realClient = HttpClientCreator.create()
        try {
            val api = YandexApi(realClient)
            val parser = YandexPlaylistUrlParser()
            val mapper = YandexMapper()
            val repository = PlaylistRepositoryImpl(
                yandexApi = api,
                yandexUrlParser = parser,
                yandexMapper = mapper,
                spotifyApi = org.mromichov.guessthesong.data.remote.spotify.SpotifyApi(realClient),
                spotifyUrlParser = org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser(),
                spotifyMapper = org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper(),
                playlistTracksDao = FakePlaylistTracksDao(),
                coverStorage = NoOpPlaylistCoverStorage,
            )
            val url = "https://music.yandex.ru/users/yamusic-top/playlists/1000"

            val result = repository.fetchPlaylistByUrl(url)

            assertTrue("Expected success response, but got: ${result.exceptionOrNull()}", result.isSuccess)
            val playlist = result.getOrThrow().content

            println("=== LIVE YANDEX PLAYLIST ===")
            println("Title: ${playlist.title}")
            println("Total tracks: ${playlist.tracks.size}")
            println("First 5 tracks:")
            playlist.tracks.take(5).forEachIndexed { index, track ->
                println("${index + 1}. ${track.artist} - ${track.title} [${track.durationMs / 1000}s] (Cover: ${track.coverUrl})")
            }

            assertTrue("Playlist title should not be blank", playlist.title.isNotBlank())
            assertFalse("Playlist tracks should not be empty", playlist.tracks.isEmpty())

            val firstTrack = playlist.tracks.first()
            assertTrue("Track title should not be blank", firstTrack.title.isNotBlank())
            assertTrue("Track artist should not be blank", firstTrack.artist.isNotBlank())
            assertNotNull("Track cover URL should not be null", firstTrack.coverUrl)
            assertTrue("Track cover URL should start with https://", firstTrack.coverUrl?.startsWith("https://") == true)
        } finally {
            realClient.close()
        }
    }
}
