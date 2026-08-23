package org.mromichov.guessthesong.domain.model

data class GameRound(
    val track: Track,
    val trackPreview: TrackPreview,
    val wrongOptions: List<Track>,
)
