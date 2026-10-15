package org.mromichov.guessthesong.domain.model.playlist

data class RemotePlaylist(
    val source: PlaylistSource,
    val content: PlaylistContent,
)