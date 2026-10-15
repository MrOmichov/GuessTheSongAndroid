package org.mromichov.guessthesong.domain.repository

import org.mromichov.guessthesong.domain.model.Playlist

interface PlaylistRepository {
    suspend fun fetchPlaylistByUrl(url: String): Result<Playlist>
    suspend fun getPlaylistByUrl(url: String): Result<Playlist>
    suspend fun getSavedPlaylists(): Result<List<Playlist>>
    suspend fun getSavedPlaylist(id: Long): Result<Playlist>
    suspend fun findSavedPlaylistByTitle(title: String): Result<Playlist?>
    suspend fun savePlaylist(playlist: Playlist): Result<Playlist>
    suspend fun replacePlaylist(existingId: Long, playlist: Playlist): Result<Playlist>
    suspend fun deletePlaylist(id: Long): Result<Unit>
}
