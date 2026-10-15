package org.mromichov.guessthesong.domain.repository

import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.RemotePlaylist

interface PlaylistRepository {
    suspend fun fetchPlaylistByUrl(url: String): Result<RemotePlaylist>
    suspend fun getLocalPlaylists(): Result<List<LocalPlaylist>>
    suspend fun getLocalPlaylist(id: Long): Result<LocalPlaylist>
    suspend fun findLocalPlaylistByTitle(title: String): Result<LocalPlaylist?>
    suspend fun savePlaylist(playlistContent: PlaylistContent): Result<LocalPlaylist>
    suspend fun replacePlaylist(existingId: Long, playlistContent: PlaylistContent): Result<LocalPlaylist>
    suspend fun deletePlaylist(id: Long): Result<Unit>
}
