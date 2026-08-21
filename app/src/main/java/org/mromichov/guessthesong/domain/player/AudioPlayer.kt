package org.mromichov.guessthesong.domain.player

import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val currentMs: StateFlow<Long>
    val durationMs: StateFlow<Long>

    fun play(url: String, startMs: Long = 0L, endMs: Long)
    fun stop()
    fun release()
}