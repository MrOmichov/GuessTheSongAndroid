package org.mromichov.guessthesong.data.remote.spotify

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

class SpotifyLiveIntegrationTest {

    private val clientId = System.getenv("SPOTIFY_CLIENT_ID").orEmpty()
    private val clientSecret = System.getenv("SPOTIFY_CLIENT_SECRET").orEmpty()

    @Test
    fun fetchLive_spotifyPlaylist_returnsRealTracks() = runTest {
        // Skip test if credentials are not configured in environment
        Assume.assumeTrue(
            "Skipping live test: SPOTIFY_CLIENT_ID and SPOTIFY_CLIENT_SECRET are not set",
            clientId.isNotBlank() && clientSecret.isNotBlank()
        )

        val config = SpotifyConfig(clientId = clientId, clientSecret = clientSecret)
        val api = SpotifyApi(config)
        val parser = SpotifyPlaylistUrlParser()
        val mapper = SpotifyMapper()

        val url = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
        val target = parser.parse(url) as? SpotifyPlaylistTarget.Playlist
        assertNotNull("URL parsing should succeed", target)

        val spotifyTracks = api.getPlaylistTracks(target!!.id)
        assertNotNull("Playlist tracks should be fetched successfully", spotifyTracks)
        assertFalse("Playlist tracks should not be empty", spotifyTracks.isEmpty())

        val domainPlaylist = mapper.toDomain(playlistId = target.id, tracks = spotifyTracks)

        println("=== LIVE SPOTIFY PLAYLIST ===")
        println("Title: ${domainPlaylist.title}")
        println("Total tracks: ${domainPlaylist.tracks.size}")
        println("First 5 tracks:")
        domainPlaylist.tracks.take(5).forEachIndexed { index, track ->
            println("${index + 1}. ${track.artist} - ${track.title} [${track.durationMs / 1000}s] (Cover: ${track.coverUrl})")
        }

        assertTrue("Playlist title should not be blank", domainPlaylist.title.isNotBlank())
        assertFalse("Playlist tracks should not be empty", domainPlaylist.tracks.isEmpty())

        val firstTrack = domainPlaylist.tracks.first()
        assertTrue("Track title should not be blank", firstTrack.title.isNotBlank())
        assertTrue("Track artist should not be blank", firstTrack.artist.isNotBlank())
    }
}
