package org.mromichov.guessthesong.domain.model

data class PlaylistSource(
    val provider: PlaylistProvider,
    val externalId: String,
)
