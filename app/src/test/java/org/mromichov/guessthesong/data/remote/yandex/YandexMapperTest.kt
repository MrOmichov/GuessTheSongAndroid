package org.mromichov.guessthesong.data.remote.yandex

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mromichov.guessthesong.data.remote.yandex.dto.*
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider

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
                tracks = tracksList,
                cover = YandexPlaylistCoverDto(uri = "avatars.yandex.net/playlist/%%")
            )
        )

        val playlist = mapper.toDomain("playlist-id-1", response)

        assertEquals(PlaylistProvider.YANDEX_MUSIC, playlist.source.provider)
        assertEquals("playlist-id-1", playlist.source.externalId)
        assertEquals("Rock Classics", playlist.content.title)
        assertEquals("https://avatars.yandex.net/playlist/400x400", playlist.content.coverUrl)
        assertEquals(20, playlist.content.tracks.size)
        assertEquals("Track 0", playlist.content.tracks[0].title)
    }

    @Test
    fun toDomain_apiPlaylistResponse_mapsToPlaylistDomain() {
        val response = YandexApiPlaylistResponseDto(
            result = YandexApiPlaylistDto(
                title = "Pop Hits",
                ogImage = "https://avatars.yandex.net/pop/%%",
                tracks = listOf(
                    YandexApiTrackContainerDto(
                        id = 1,
                        track = YandexTrackDto(
                            id = "2",
                            title = "Track 2",
                            artists = listOf(YandexArtistDto(name = "Singer B"))
                        )
                    )
                )
            )
        )

        val playlist = mapper.toDomain("uuid-123", response)

        assertEquals(PlaylistProvider.YANDEX_MUSIC, playlist.source.provider)
        assertEquals("uuid-123", playlist.source.externalId)
        assertEquals("Pop Hits", playlist.content.title)
        assertEquals("https://avatars.yandex.net/pop/400x400", playlist.content.coverUrl)
        assertEquals(1, playlist.content.tracks.size)
        assertEquals("Track 2", playlist.content.tracks[0].title)
    }

    @Test
    fun playlistCoverDto_serializesApiAndWebCoverFieldsAndToleratesMissingFields() {
        val json = Json { ignoreUnknownKeys = true }
        val api = json.decodeFromString<YandexApiPlaylistResponseDto>(
            """{"result":{"title":"API playlist","cover":{"uri":"avatars.yandex.net/api/%%"},"ogImage":"https://avatars.yandex.net/api-og/%%","tracks":[]}}""",
        )
        val web = json.decodeFromString<YandexWebPlaylistResponseDto>(
            """{"playlist":{"title":"Web playlist","ogImage":"avatars.yandex.net/web/%%","tracks":[]}}""",
        )
        val withoutCover = json.decodeFromString<YandexApiPlaylistResponseDto>(
            """{"result":{"title":"No cover","tracks":[]}}""",
        )

        assertEquals("https://avatars.yandex.net/api/400x400", mapper.toDomain("api", api).content.coverUrl)
        assertEquals("https://avatars.yandex.net/web/400x400", mapper.toDomain("web", web).content.coverUrl)
        assertNull(mapper.toDomain("legacy", withoutCover).content.coverUrl)
    }
}
