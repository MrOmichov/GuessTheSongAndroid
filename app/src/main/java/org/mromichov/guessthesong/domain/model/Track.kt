package org.mromichov.guessthesong.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val durationMs: Long
)
