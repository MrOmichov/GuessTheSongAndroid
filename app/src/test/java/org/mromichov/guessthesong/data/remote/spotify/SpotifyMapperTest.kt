package org.mromichov.guessthesong.data.remote.spotify

import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.Track
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyMapperTest {

    private val mapper = SpotifyMapper()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun toDomain_singleTrack_formatsArtistsAndCoverUrlCorrectly() {
        val trackJson = """
        {
            "id": "track_123",
            "name": "Starboy",
            "popularity": 95,
            "available_markets": [],
            "external_ids": {},
            "artists": [
                {
                    "id": "artist1",
                    "name": "The Weeknd",
                    "href": "href1",
                    "uri": "spotify:artist:artist1",
                    "type": "artist",
                    "external_urls": {}
                },
                {
                    "id": "artist2",
                    "name": "Daft Punk",
                    "href": "href2",
                    "uri": "spotify:artist:artist2",
                    "type": "artist",
                    "external_urls": {}
                }
            ],
            "album": {
                "id": "album_123",
                "name": "Starboy",
                "href": "href",
                "uri": "spotify:album:album_123",
                "album_type": "album",
                "type": "album",
                "artists": [],
                "images": [
                    {
                        "url": "https://i.scdn.co/image/ab67616d0000b273starboy",
                        "height": 640,
                        "width": 640
                    }
                ],
                "external_urls": {}
            },
            "duration_ms": 230000,
            "preview_url": "https://p.scdn.co/mp3-preview/sample.mp3",
            "disc_number": 1,
            "track_number": 1,
            "explicit": false,
            "is_playable": true,
            "href": "href",
            "type": "track",
            "uri": "spotify:track:track_123",
            "is_local": false,
            "external_urls": {}
        }
        """.trimIndent()

        val spotifyTrack = json.decodeFromString<Track>(trackJson)
        val domainTrack = mapper.toDomain(spotifyTrack)

        assertEquals("track_123", domainTrack.id)
        assertEquals("Starboy", domainTrack.title)
        assertEquals("The Weeknd, Daft Punk", domainTrack.artist)
        assertEquals("https://i.scdn.co/image/ab67616d0000b273starboy", domainTrack.coverUrl)
        assertEquals(230000L, domainTrack.durationMs)
    }

    @Test
    fun toDomain_playlist_mapsAllValidTracks() {
        val playlistJson = """
        {
            "id": "rock_classics_id",
            "name": "Rock Classics",
            "description": "Rock Classics",
            "href": "href",
            "uri": "spotify:playlist:rock_classics_id",
            "public": true,
            "collaborative": false,
            "snapshot_id": "snapshot_1",
            "type": "playlist",
            "followers": {
                "href": null,
                "total": 100
            },
            "images": [],
            "owner": {
                "id": "spotify",
                "display_name": "Spotify",
                "href": "href",
                "type": "user",
                "uri": "spotify:user:spotify",
                "external_urls": {}
            },
            "tracks": {
                "href": "href",
                "items": [
                    {
                        "is_local": false,
                        "track": {
                            "id": "bohemian_rhapsody",
                            "name": "Bohemian Rhapsody",
                            "popularity": 90,
                            "available_markets": [],
                            "external_ids": {},
                            "artists": [
                                {
                                    "id": "artist1",
                                    "name": "Queen",
                                    "href": "href",
                                    "uri": "spotify:artist:artist1",
                                    "type": "artist",
                                    "external_urls": {}
                                }
                            ],
                            "album": {
                                "id": "album_queen",
                                "name": "A Night at the Opera",
                                "href": "href",
                                "uri": "spotify:album:album_queen",
                                "album_type": "album",
                                "type": "album",
                                "artists": [],
                                "images": [
                                    {
                                        "url": "https://i.scdn.co/image/queen_cover",
                                        "height": 640,
                                        "width": 640
                                    }
                                ],
                                "external_urls": {}
                            },
                            "duration_ms": 354000,
                            "preview_url": "https://p.scdn.co/mp3-preview/bohemian.mp3",
                            "disc_number": 1,
                            "track_number": 1,
                            "explicit": false,
                            "is_playable": true,
                            "href": "href",
                            "type": "track",
                            "uri": "spotify:track:bohemian_rhapsody",
                            "is_local": false,
                            "external_urls": {}
                        }
                    }
                ],
                "limit": 100,
                "offset": 0,
                "total": 1
            },
            "external_urls": {}
        }
        """.trimIndent()

        val playlist = json.decodeFromString<Playlist>(playlistJson)
        val domainPlaylist = mapper.toDomain(playlist)

        assertEquals("rock_classics_id", domainPlaylist.id)
        assertEquals("Rock Classics", domainPlaylist.title)
        assertEquals(1, domainPlaylist.tracks.size)
        assertEquals("Bohemian Rhapsody", domainPlaylist.tracks.first().title)
        assertEquals("Queen", domainPlaylist.tracks.first().artist)
    }

    @Test
    fun toPreviewDomain_trackWithPreview_returnsTrackPreview() {
        val trackJson = """
        {
            "id": "lose_yourself",
            "name": "Lose Yourself",
            "popularity": 88,
            "available_markets": [],
            "external_ids": {},
            "artists": [
                {
                    "id": "artist1",
                    "name": "Eminem",
                    "href": "href",
                    "uri": "spotify:artist:artist1",
                    "type": "artist",
                    "external_urls": {}
                }
            ],
            "album": {
                "id": "album_8mile",
                "name": "8 Mile",
                "href": "href",
                "uri": "spotify:album:album_8mile",
                "album_type": "album",
                "type": "album",
                "artists": [],
                "images": [
                    {
                        "url": "https://i.scdn.co/image/8mile_cover",
                        "height": 640,
                        "width": 640
                    }
                ],
                "external_urls": {}
            },
            "duration_ms": 326000,
            "preview_url": "https://p.scdn.co/mp3-preview/lose_yourself.mp3",
            "disc_number": 1,
            "track_number": 1,
            "explicit": true,
            "is_playable": true,
            "href": "href",
            "type": "track",
            "uri": "spotify:track:lose_yourself",
            "is_local": false,
            "external_urls": {}
        }
        """.trimIndent()

        val spotifyTrack = json.decodeFromString<Track>(trackJson)
        val preview = mapper.toPreviewDomain(spotifyTrack)

        assertNotNull(preview)
        assertEquals("Lose Yourself", preview?.trackName)
        assertEquals("Eminem", preview?.artistName)
        assertEquals("https://p.scdn.co/mp3-preview/lose_yourself.mp3", preview?.previewUrl)
        assertEquals("https://i.scdn.co/image/8mile_cover", preview?.artworkUrl)
    }

    @Test
    fun toPreviewDomain_trackWithoutPreview_returnsNull() {
        val trackJson = """
        {
            "id": "lose_yourself",
            "name": "Lose Yourself",
            "popularity": 88,
            "available_markets": [],
            "external_ids": {},
            "artists": [
                {
                    "id": "artist1",
                    "name": "Eminem",
                    "href": "href",
                    "uri": "spotify:artist:artist1",
                    "type": "artist",
                    "external_urls": {}
                }
            ],
            "album": {
                "id": "album_8mile",
                "name": "8 Mile",
                "href": "href",
                "uri": "spotify:album:album_8mile",
                "album_type": "album",
                "type": "album",
                "artists": [],
                "images": [],
                "external_urls": {}
            },
            "duration_ms": 326000,
            "preview_url": null,
            "disc_number": 1,
            "track_number": 1,
            "explicit": true,
            "is_playable": true,
            "href": "href",
            "type": "track",
            "uri": "spotify:track:lose_yourself",
            "is_local": false,
            "external_urls": {}
        }
        """.trimIndent()

        val spotifyTrack = json.decodeFromString<Track>(trackJson)
        val preview = mapper.toPreviewDomain(spotifyTrack)

        assertNull(preview)
    }
}
