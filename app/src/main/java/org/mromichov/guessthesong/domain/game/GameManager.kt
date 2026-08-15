package org.mromichov.guessthesong.domain.game

import org.mromichov.guessthesong.domain.model.Playlist
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameManager @Inject constructor() {
    var currentPlaylist: Playlist? = null
}