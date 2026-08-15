package org.mromichov.guessthesong.core.di

import dagger.Binds
import dagger.Module
import org.mromichov.guessthesong.data.repository.PlaylistRepositoryImpl
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import javax.inject.Singleton

@Module
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        impl: PlaylistRepositoryImpl
    ): PlaylistRepository
}
