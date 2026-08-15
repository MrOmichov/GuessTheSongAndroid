package org.mromichov.guessthesong.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mromichov.guessthesong.domain.player.AudioPlayer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
@Singleton
class AudioPlayerImpl @Inject constructor(
    private val context: Context
) : AudioPlayer {
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentMs = MutableStateFlow(0L)
    override val currentMs: StateFlow<Long> = _currentMs.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                _currentMs.value = player.currentPosition
                delay(50.milliseconds)
            }
        }
    }
    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private val databaseProvider: DatabaseProvider by lazy {
        StandaloneDatabaseProvider(context)
    }

    private val cache: SimpleCache by lazy {
        val cacheDir = File(context.cacheDir, "audio_previews")
        val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024)
        SimpleCache(cacheDir, evictor, databaseProvider)
    }

    private val cacheDataSourceFactory by lazy {
        val upstreamFactory = DefaultHttpDataSource.Factory()
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private val player: ExoPlayer by lazy {
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(cacheDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) {
                            startProgressPolling()
                        } else {
                            stopProgressPolling()
                            _currentMs.value = player.currentPosition
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            _currentMs.value = player.duration
                        }
                    }
                })
            }
    }


    override fun play(url: String, startMs: Long) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem, startMs)
        player.prepare()
        player.play()
    }

    override fun stop() {
        player.stop()
        stopProgressPolling()
        _currentMs.value = 0L
    }

    override fun release() {
        stopProgressPolling()
        scope.cancel()
        player.release()
    }
}