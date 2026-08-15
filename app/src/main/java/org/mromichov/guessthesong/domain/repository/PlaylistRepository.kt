package org.mromichov.guessthesong.domain.repository

import org.mromichov.guessthesong.domain.model.Playlist

interface PlaylistRepository {
    suspend fun getPlaylistByUrl(url: String): Result<Playlist>
}