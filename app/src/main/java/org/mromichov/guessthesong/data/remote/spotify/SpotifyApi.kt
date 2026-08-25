package org.mromichov.guessthesong.data.remote.spotify

import com.adamratzman.spotify.SpotifyAppApi
import com.adamratzman.spotify.models.Playlist
import com.adamratzman.spotify.models.PlaylistTrack
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.spotifyAppApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyApi @Inject constructor(
    private val config: SpotifyConfig
) {
    private val mutex = Mutex()
    private var apiInstance: SpotifyAppApi? = null

    suspend fun getApi(): SpotifyAppApi {
        apiInstance?.let { return it }
        return mutex.withLock {
            apiInstance ?: spotifyAppApi(
                clientId = config.clientId,
                clientSecret = config.clientSecret
            ).build().also { apiInstance = it }
        }
    }

    suspend fun getPlaylist(playlistId: String): Playlist? {
        val api = getApi()
        return api.playlists.getPlaylist(playlistId)
    }

    suspend fun getPlaylistTracks(playlistId: String, limit: Int = 50): List<PlaylistTrack> {
        val api = getApi()
        val paging = api.playlists.getPlaylistTracks(playlist = playlistId, limit = limit)
        return paging.items
    }

    suspend fun searchTrack(query: String, limit: Int = 5): List<Track> {
        val api = getApi()
        val searchResult = api.search.searchTrack(query = query, limit = limit)
        return searchResult.items
    }
}
