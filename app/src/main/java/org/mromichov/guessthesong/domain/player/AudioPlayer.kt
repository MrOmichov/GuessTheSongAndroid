package org.mromichov.guessthesong.domain.player

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val currentMs: StateFlow<Long>

    fun play(url: String, startMs: Long = 0L)
    fun stop()
    fun release()
}