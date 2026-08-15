package org.mromichov.guessthesong.core.di

import dagger.Component
import dagger.Module
import org.mromichov.guessthesong.MainActivity

@Component(modules = [NetworkModule::class, RepositoryModule::class, AudioPlayerModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)
}