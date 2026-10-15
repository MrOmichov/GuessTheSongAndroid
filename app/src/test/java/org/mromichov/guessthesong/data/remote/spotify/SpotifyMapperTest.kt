package org.mromichov.guessthesong.data.remote.spotify

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.mromichov.guessthesong.data.remote.spotify.dto.*
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider

class SpotifyMapperTest {

    private val mapper = SpotifyMapper()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun toDomain_singleTrack_formatsArtistAndDurationCorrectly() {
        val trackDto = SpotifyWebTrackDto(
            uri = "spotify:track:2vzfV6LgfAWtv7JeSoEPFO",
            uid = "02f3ca667955def4",
            title = "The Silent Man",
            subtitle = "Dream Theater",
            duration = 227733,
            audioPreview = SpotifyAudioPreviewDto(
                format = "MP3_96",
                url = "https://p.scdn.co/mp3-preview/sample.mp3"
            )
        )

        val domainTrack = mapper.toDomain(dto = trackDto, coverUrl = "https://cover.url")

        assertEquals("The Silent Man", domainTrack.title)
        assertEquals("Dream Theater", domainTrack.artist)
        assertEquals("https://cover.url", domainTrack.coverUrl)
        assertEquals(227733L, domainTrack.durationMs)
    }

    @Test
    fun toDomain_embedDto_mapsAll20TracksAndPlaylistTitle() {
        val tracks = List(20) { index ->
            SpotifyWebTrackDto(
                uri = "spotify:track:track_$index",
                title = "Track $index",
                subtitle = "Artist $index",
                duration = 200000L
            )
        }

        val embedDto = SpotifyWebEmbedDto(
            props = SpotifyPropsDto(
                pageProps = SpotifyPagePropsDto(
                    state = SpotifyStateDto(
                        data = SpotifyDataDto(
                            entity = SpotifyEntityDto(
                                id = "4lcPGoGgUtNpexPogG2ctm",
                                name = "Dream Theater ballads",
                                coverArt = SpotifyCoverArtDto(
                                    sources = listOf(
                                        SpotifyImageSourceDto(url = "https://mosaic.scdn.co/640/cover.jpg")
                                    )
                                ),
                                trackList = tracks
                            )
                        )
                    )
                )
            )
        )

        val domainPlaylist = mapper.toDomain(playlistExternalId = "4lcPGoGgUtNpexPogG2ctm", dto = embedDto)

        assertEquals(PlaylistProvider.SPOTIFY, domainPlaylist.source.provider)
        assertEquals("4lcPGoGgUtNpexPogG2ctm", domainPlaylist.source.externalId)
        assertEquals("Dream Theater ballads", domainPlaylist.content.title)
        assertEquals(20, domainPlaylist.content.tracks.size)
        assertEquals("Track 0", domainPlaylist.content.tracks.first().title)
        assertEquals("Artist 0", domainPlaylist.content.tracks.first().artist)
        assertEquals("https://mosaic.scdn.co/640/cover.jpg", domainPlaylist.content.tracks.first().coverUrl)
    }

    @Test
    fun toDomain_fiveTracks_mapsSuccessfully() {
        val tracks = List(5) { index ->
            SpotifyWebTrackDto(
                uri = "spotify:track:track_$index",
                title = "Track $index",
                subtitle = "Artist $index",
                duration = 200000L
            )
        }

        val embedDto = SpotifyWebEmbedDto(
            props = SpotifyPropsDto(
                pageProps = SpotifyPagePropsDto(
                    state = SpotifyStateDto(
                        data = SpotifyDataDto(
                            entity = SpotifyEntityDto(
                                id = "short_playlist",
                                name = "Short Playlist",
                                trackList = tracks
                            )
                        )
                    )
                )
            )
        )

        val domainPlaylist = mapper.toDomain(playlistExternalId = "short_playlist", dto = embedDto)

        assertEquals(5, domainPlaylist.content.tracks.size)
        assertEquals("Track 0", domainPlaylist.content.tracks.first().title)
        assertEquals("Artist 0", domainPlaylist.content.tracks.first().artist)
    }

    @Test
    fun toPreviewDomain_trackWithPreview_returnsTrackPreview() {
        val trackDto = SpotifyWebTrackDto(
            uri = "spotify:track:123",
            title = "Lose Yourself",
            subtitle = "Eminem",
            audioPreview = SpotifyAudioPreviewDto(url = "https://preview.mp3")
        )

        val preview = mapper.toPreviewDomain(trackDto, coverUrl = "https://cover.jpg")

        assertNotNull(preview)
        assertEquals("Lose Yourself", preview?.title)
        assertEquals("Eminem", preview?.artist)
        assertEquals("https://preview.mp3", preview?.trackPreviewUrl)
        assertEquals("https://cover.jpg", preview?.coverUrl)
    }

    @Test
    fun toPreviewDomain_trackWithoutPreview_returnsNull() {
        val trackDto = SpotifyWebTrackDto(
            uri = "spotify:track:123",
            title = "Lose Yourself",
            subtitle = "Eminem",
            audioPreview = null
        )

        val preview = mapper.toPreviewDomain(trackDto, coverUrl = "https://cover.jpg")

        assertNull(preview)
    }
}
