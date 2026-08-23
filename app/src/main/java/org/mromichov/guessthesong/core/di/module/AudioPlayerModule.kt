package org.mromichov.guessthesong.core.di.module

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.Binds
import dagger.Module
import org.mromichov.guessthesong.data.player.AudioPlayerImpl
import org.mromichov.guessthesong.domain.player.AudioPlayer
import javax.inject.Singleton

@Module
interface AudioPlayerModule {

    @OptIn(UnstableApi::class)
    @Binds
    @Singleton
    fun bindAudioPlayer(
        audioPlayerImpl: AudioPlayerImpl
    ): AudioPlayer

}