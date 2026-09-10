package org.mromichov.guessthesong.domain.model

data class TrackPreview(
    val trackName: String,
    val artistName: String,
    val previewUrl: String,
    val artworkUrl: String?
)