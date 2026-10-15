package org.mromichov.guessthesong.domain.model.playlist

data class PlaylistSource(
    val provider: PlaylistProvider,
    val externalId: String,
)
