package org.mromichov.guessthesong.data.remote.spotify

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mromichov.guessthesong.core.exception.TrackCountException
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyAudioPreviewDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyCoverArtDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyDataDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyEntityDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyImageSourceDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyPagePropsDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyPropsDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyStateDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyWebEmbedDto
import org.mromichov.guessthesong.data.remote.spotify.dto.SpotifyWebTrackDto

class SpotifyMapperTest {

    private val mapper = SpotifyMapper()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun toDomain_singleTrack_formatsIdArtistAndDurationCorrectly() {
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

        assertEquals("2vzfV6LgfAWtv7JeSoEPFO", domainTrack.id)
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

        val domainPlaylist = mapper.toDomain(playlistId = "4lcPGoGgUtNpexPogG2ctm", dto = embedDto)

        assertEquals("4lcPGoGgUtNpexPogG2ctm", domainPlaylist.id)
        assertEquals("Dream Theater ballads", domainPlaylist.title)
        assertEquals(20, domainPlaylist.tracks.size)
        assertEquals("Track 0", domainPlaylist.tracks.first().title)
        assertEquals("Artist 0", domainPlaylist.tracks.first().artist)
        assertEquals("https://mosaic.scdn.co/640/cover.jpg", domainPlaylist.tracks.first().coverUrl)
    }

    @Test(expected = TrackCountException::class)
    fun toDomain_lessThan20Tracks_throwsTrackCountException() {
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

        mapper.toDomain(playlistId = "short_playlist", dto = embedDto)
    }

    @Test
    fun toPreviewDomain_trackWithPreview_returnsTrackPreview() {
        val trackDto = SpotifyWebTrackDto(
            uri = "spotify:track:123",
            title = "Lose Yourself",
            subtitle = "Eminem",
            audioPreview = SpotifyAudioPreviewDto(url = "https://preview.mp3")
        )

        val preview = mapper.toPreviewDomain(trackDto, artworkUrl = "https://cover.jpg")

        assertNotNull(preview)
        assertEquals("Lose Yourself", preview?.trackName)
        assertEquals("Eminem", preview?.artistName)
        assertEquals("https://preview.mp3", preview?.previewUrl)
        assertEquals("https://cover.jpg", preview?.artworkUrl)
    }

    @Test
    fun toPreviewDomain_trackWithoutPreview_returnsNull() {
        val trackDto = SpotifyWebTrackDto(
            uri = "spotify:track:123",
            title = "Lose Yourself",
            subtitle = "Eminem",
            audioPreview = null
        )

        val preview = mapper.toPreviewDomain(trackDto, artworkUrl = "https://cover.jpg")

        assertNull(preview)
    }
}
