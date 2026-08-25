package org.mromichov.guessthesong.data.remote.yandex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexApiPlaylistDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexApiPlaylistResponseDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexApiTrackContainerDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexArtistDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexTrackDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexWebPlaylistDto
import org.mromichov.guessthesong.data.remote.yandex.dto.YandexWebPlaylistResponseDto

class YandexMapperTest {

    private val mapper = YandexMapper()

    @Test
    fun toDomain_singleTrack_formatsArtistsAndCoverUrlCorrectly() {
        val dto = YandexTrackDto(
            id = "101",
            title = "In the End",
            artists = listOf(
                YandexArtistDto(1, "Linkin Park"),
                YandexArtistDto(2, "Jay-Z")
            ),
            coverUri = "avatars.yandex.net/get-music-content/101/%%",
            durationMs = 216000
        )

        val domain = mapper.toDomain(dto)

        assertEquals("101", domain.id)
        assertEquals("In the End", domain.title)
        assertEquals("Linkin Park, Jay-Z", domain.artist)
        assertEquals("https://avatars.yandex.net/get-music-content/101/400x400", domain.coverUrl)
        assertEquals(216000L, domain.durationMs)
    }

    @Test
    fun toDomain_coverUriWithoutHttps_prependsHttps() {
        val dto = YandexTrackDto(
            id = "102",
            title = "Numb",
            coverUri = "avatars.yandex.net/get-music-content/102/%%"
        )
        val domain = mapper.toDomain(dto)
        assertEquals("https://avatars.yandex.net/get-music-content/102/400x400", domain.coverUrl)
    }

    @Test
    fun toDomain_coverUriWithHttps_doesNotDuplicateHttps() {
        val dto = YandexTrackDto(
            id = "103",
            title = "Faint",
            coverUri = "https://avatars.yandex.net/get-music-content/103/%%"
        )
        val domain = mapper.toDomain(dto)
        assertEquals("https://avatars.yandex.net/get-music-content/103/400x400", domain.coverUrl)
    }

    @Test
    fun toDomain_fallbackToOgImage_whenCoverUriIsNull() {
        val dto = YandexTrackDto(
            id = "104",
            title = "Papercut",
            coverUri = null,
            ogImage = "avatars.yandex.net/get-music-content/104/%%"
        )
        val domain = mapper.toDomain(dto)
        assertEquals("https://avatars.yandex.net/get-music-content/104/400x400", domain.coverUrl)
    }

    @Test
    fun toDomain_nullCovers_returnsNullCoverUrl() {
        val dto = YandexTrackDto(
            id = "105",
            title = "Crawling",
            coverUri = null,
            ogImage = null
        )
        val domain = mapper.toDomain(dto)
        assertNull(domain.coverUrl)
    }

    @Test
    fun toDomain_webPlaylistResponse_mapsToPlaylistDomain() {
        val tracksList = List(20) { index ->
            YandexTrackDto(id = "$index", title = "Track $index", artists = listOf(YandexArtistDto(name = "Band A")))
        }
        val response = YandexWebPlaylistResponseDto(
            playlist = YandexWebPlaylistDto(
                title = "Rock Classics",
                tracks = tracksList
            )
        )

        val playlist = mapper.toDomain("playlist-id-1", response)

        assertEquals("playlist-id-1", playlist.id)
        assertEquals("Rock Classics", playlist.title)
        assertEquals(20, playlist.tracks.size)
        assertEquals("Track 0", playlist.tracks[0].title)
    }

    @Test
    fun toDomain_apiPlaylistResponse_mapsToPlaylistDomain() {
        val response = YandexApiPlaylistResponseDto(
            result = YandexApiPlaylistDto(
                title = "Pop Hits",
                tracks = listOf(
                    YandexApiTrackContainerDto(
                        id = 1,
                        track = YandexTrackDto(id = "2", title = "Track 2", artists = listOf(YandexArtistDto(name = "Singer B")))
                    )
                )
            )
        )

        val playlist = mapper.toDomain("uuid-123", response)

        assertEquals("uuid-123", playlist.id)
        assertEquals("Pop Hits", playlist.title)
        assertEquals(1, playlist.tracks.size)
        assertEquals("Track 2", playlist.tracks[0].title)
    }
}
