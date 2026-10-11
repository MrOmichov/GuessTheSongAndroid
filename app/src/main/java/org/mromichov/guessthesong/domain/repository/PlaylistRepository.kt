package org.mromichov.guessthesong.domain.repository

import org.mromichov.guessthesong.domain.model.Playlist

interface PlaylistRepository {
    suspend fun getPlaylistByUrl(url: String): Result<Playlist>
    suspend fun getSavedPlaylists(): Result<List<Playlist>>
    suspend fun getSavedPlaylist(id: Long): Result<Playlist>
}
