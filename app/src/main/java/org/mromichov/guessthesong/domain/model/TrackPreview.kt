package org.mromichov.guessthesong.domain.model

data class TrackPreview(
    val title: String,
    val artist: String,
    val trackPreviewUrl: String,
    val coverUrl: String?
)
