package org.mromichov.guessthesong.data.remote.spotify

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.core.network.HttpClientCreator

class SpotifyLiveIntegrationTest {

    private val realClient = HttpClientCreator.create()
    private val api = SpotifyApi(realClient)
    private val parser = SpotifyPlaylistUrlParser()
    private val mapper = SpotifyMapper()

    @Test
    fun fetchLive_spotifyUserPlaylist_returnsRealTracks() = runTest {
        val url = "https://open.spotify.com/playlist/4lcPGoGgUtNpexPogG2ctm"
        val target = parser.parse(url) as? SpotifyPlaylistTarget.Playlist
        assertNotNull("URL parsing should succeed", target)

        val spotifyEmbedDto = api.getPlaylist(target!!.id)
        assertNotNull("Playlist embed should be fetched successfully", spotifyEmbedDto)

        val domainPlaylist = mapper.toDomain(playlistId = target.id, dto = spotifyEmbedDto)

        println("=== LIVE SPOTIFY USER PLAYLIST ===")
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

    @Test
    fun fetchLive_spotifyEditorialPlaylist_returnsRealTracks() = runTest {
        val url = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
        val target = parser.parse(url) as? SpotifyPlaylistTarget.Playlist
        assertNotNull("URL parsing should succeed", target)

        val spotifyEmbedDto = api.getPlaylist(target!!.id)
        assertNotNull("Playlist embed should be fetched successfully", spotifyEmbedDto)

        val domainPlaylist = mapper.toDomain(playlistId = target.id, dto = spotifyEmbedDto)

        println("=== LIVE SPOTIFY EDITORIAL PLAYLIST ===")
        println("Title: ${domainPlaylist.title}")
        println("Total tracks: ${domainPlaylist.tracks.size}")

        assertTrue("Playlist title should not be blank", domainPlaylist.title.isNotBlank())
        assertFalse("Playlist tracks should not be empty", domainPlaylist.tracks.isEmpty())
    }
}
