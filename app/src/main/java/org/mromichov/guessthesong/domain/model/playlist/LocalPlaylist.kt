package org.mromichov.guessthesong.domain.model.playlist

data class LocalPlaylist(
    val id: Long,
    val content: PlaylistContent,
    val coverFileName: String? = null,
    val provider: PlaylistProvider? = null
)
