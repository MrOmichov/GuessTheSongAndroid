package org.mromichov.guessthesong.data.remote.yandex

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.core.network.HttpClientCreator
import org.mromichov.guessthesong.data.repository.PlaylistRepositoryImpl

class YandexLiveIntegrationTest {

    private val realClient = HttpClientCreator.create()
    private val api = YandexApi(realClient)
    private val parser = YandexPlaylistUrlParser()
    private val mapper = YandexMapper()
    private val repository = PlaylistRepositoryImpl(api, parser, mapper)

    @Test
    fun fetchLive_yandexTopChartPlaylist_returnsRealTracks() = runTest {
        val url = "https://music.yandex.ru/users/yamusic-top/playlists/1000"

        val result = repository.getPlaylistByUrl(url)

        assertTrue("Expected success response, but got: ${result.exceptionOrNull()}", result.isSuccess)
        val playlist = result.getOrThrow()

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
    }
}
