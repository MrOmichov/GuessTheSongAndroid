package org.mromichov.guessthesong.domain.model.playlist

import org.mromichov.guessthesong.domain.model.Track

data class PlaylistContent(
    val title: String,
    val tracks: List<Track>,
    val coverUrl: String? = null
)
