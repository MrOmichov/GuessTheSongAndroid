package org.mromichov.guessthesong.core.di.module

import dagger.Module
import dagger.Provides
import org.mromichov.guessthesong.domain.game.GameManager
import javax.inject.Singleton

@Module
class GameModule {
    @Provides
    @Singleton
    fun provideGameManager(): GameManager {
        return GameManager()
    }
}