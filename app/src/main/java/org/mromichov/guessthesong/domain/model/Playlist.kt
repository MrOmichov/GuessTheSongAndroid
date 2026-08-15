package org.mromichov.guessthesong.domain.model

data class Playlist(
    val id: String,
    val title: String,
    val tracks: List<Track>
)
