package org.mromichov.guessthesong.data.remote.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyPlaylistUrlParserTest {

    private val parser = SpotifyPlaylistUrlParser()

    @Test
    fun parse_playlistUrl_success() {
        val url = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
        val expected = SpotifyPlaylistTarget.Playlist(id = "37i9dQZF1DXcBWIGoYBM5M")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_playlistUrl_withQueryParams_success() {
        val url = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=123456abcdef&pt=987654"
        val expected = SpotifyPlaylistTarget.Playlist(id = "37i9dQZF1DXcBWIGoYBM5M")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_playlistUrl_intlLocale_success() {
        val url = "https://open.spotify.com/intl-ru/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc"
        val expected = SpotifyPlaylistTarget.Playlist(id = "37i9dQZF1DXcBWIGoYBM5M")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_playlistUri_success() {
        val uri = "spotify:playlist:37i9dQZF1DXcBWIGoYBM5M"
        val expected = SpotifyPlaylistTarget.Playlist(id = "37i9dQZF1DXcBWIGoYBM5M")

        val result = parser.parse(uri)

        assertEquals(expected, result)
    }

    @Test
    fun parse_withoutHttpsScheme_success() {
        val url = "open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
        val expected = SpotifyPlaylistTarget.Playlist(id = "37i9dQZF1DXcBWIGoYBM5M")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_invalidUrl_returnsNull() {
        assertNull(parser.parse("https://music.yandex.ru/users/yamusic-top/playlists/1000"))
        assertNull(parser.parse("https://open.spotify.com/track/37i9dQZF1DXcBWIGoYBM5M"))
        assertNull(parser.parse("https://open.spotify.com/album/37i9dQZF1DXcBWIGoYBM5M"))
        assertNull(parser.parse("not_a_url"))
        assertNull(parser.parse(""))
    }
}
