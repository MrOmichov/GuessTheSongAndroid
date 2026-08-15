package org.mromichov.guessthesong.domain.model

data class TrackPreview(
    val trackId: Long?,
    val trackName: String,
    val artistName: String,
    val previewUrl: String,
    val artworkUrl: String?
)
