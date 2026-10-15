package org.mromichov.guessthesong.domain.model

data class RemotePlaylist(
    val source: PlaylistSource,
    val content: PlaylistContent,
)