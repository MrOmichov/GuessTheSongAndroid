package org.mromichov.guessthesong.domain.player

import androidx.media3.common.PlaybackException
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val snippetFinished: StateFlow<Boolean>
    val error: StateFlow<PlaybackException?>
    val currentMs: StateFlow<Long>
    val durationMs: StateFlow<Long>

    fun play(url: String, startMs: Long = 0L, endMs: Long)
    fun stop()
    fun release()
}