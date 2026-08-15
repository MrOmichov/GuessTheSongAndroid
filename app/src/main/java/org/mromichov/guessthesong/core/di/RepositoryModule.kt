package org.mromichov.guessthesong.core.di

import dagger.Binds
import dagger.Module
import org.mromichov.guessthesong.data.repository.PlaylistRepositoryImpl
import org.mromichov.guessthesong.data.repository.TrackPreviewRepositoryImpl
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import javax.inject.Singleton

@Module
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindTrackPreviewRepository(
        impl: TrackPreviewRepositoryImpl
    ): TrackPreviewRepository
}
