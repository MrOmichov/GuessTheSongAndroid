package org.mromichov.guessthesong.data.remote.yandex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YandexPlaylistUrlParserTest {

    private val parser = YandexPlaylistUrlParser()

    @Test
    fun parse_userPlaylist_success() {
        val url = "https://music.yandex.ru/users/yamusic-top/playlists/1000"
        val expected = YandexPlaylistTarget.UserPlaylist(owner = "yamusic-top", kind = "1000")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_userPlaylist_withQueryParams_success() {
        val url = "https://music.yandex.ru/users/user.test_123/playlists/5678?lang=ru&utm_source=share"
        val expected = YandexPlaylistTarget.UserPlaylist(owner = "user.test_123", kind = "5678")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_userPlaylist_regionalDomains_success() {
        val urlBy = "https://music.yandex.by/users/someone/playlists/42"
        val urlKz = "https://music.yandex.kz/users/someone/playlists/42"
        val urlCom = "https://music.yandex.com/users/someone/playlists/42"
        val expected = YandexPlaylistTarget.UserPlaylist(owner = "someone", kind = "42")

        assertEquals(expected, parser.parse(urlBy))
        assertEquals(expected, parser.parse(urlKz))
        assertEquals(expected, parser.parse(urlCom))
    }

    @Test
    fun parse_uuidPlaylist_success() {
        val url = "https://music.yandex.ru/playlists/72ef4280-9289-4a94-b15a-b9c2a8684bb6"
        val expected = YandexPlaylistTarget.UuidPlaylist(uuid = "72ef4280-9289-4a94-b15a-b9c2a8684bb6")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_uuidPlaylist_withQueryParams_success() {
        val url = "https://music.yandex.ru/playlists/72ef4280-9289-4a94-b15a-b9c2a8684bb6?lang=ru&extra=1"
        val expected = YandexPlaylistTarget.UuidPlaylist(uuid = "72ef4280-9289-4a94-b15a-b9c2a8684bb6")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_uuidPlaylist_withPsPrefixAndDot_success() {
        val url = "https://music.yandex.ru/playlists/ps.f4c4458b-2d70-49f9-b7b6-87bbbbcd4567"
        val expected = YandexPlaylistTarget.UuidPlaylist(uuid = "ps.f4c4458b-2d70-49f9-b7b6-87bbbbcd4567")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_singularPlaylistPath_success() {
        val urlUser = "https://music.yandex.ru/users/yamusic-top/playlist/1000"
        val urlUuid = "https://music.yandex.ru/playlist/ps.f4c4458b-2d70-49f9-b7b6-87bbbbcd4567"

        assertEquals(YandexPlaylistTarget.UserPlaylist("yamusic-top", "1000"), parser.parse(urlUser))
        assertEquals(YandexPlaylistTarget.UuidPlaylist("ps.f4c4458b-2d70-49f9-b7b6-87bbbbcd4567"), parser.parse(urlUuid))
    }

    @Test
    fun parse_withoutHttpsScheme_success() {
        val url = "music.yandex.ru/playlists/ps.f4c4458b-2d70-49f9-b7b6-87bbbbcd4567"
        val expected = YandexPlaylistTarget.UuidPlaylist(uuid = "ps.f4c4458b-2d70-49f9-b7b6-87bbbbcd4567")

        val result = parser.parse(url)

        assertEquals(expected, result)
    }

    @Test
    fun parse_invalidUrl_returnsNull() {
        assertNull(parser.parse("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
        assertNull(parser.parse("not_a_url"))
        assertNull(parser.parse("https://music.yandex.ru/artist/123"))
    }
}
