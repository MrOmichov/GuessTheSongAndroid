package org.mromichov.guessthesong.core.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import org.mromichov.guessthesong.MainActivity
import org.mromichov.guessthesong.core.di.module.*
import javax.inject.Singleton

@Singleton
@Component(modules = [NetworkModule::class, RepositoryModule::class, AudioPlayerModule::class, ViewModelModule::class])
interface AppComponent {
    fun inject(mainActivity: MainActivity)

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): AppComponent
    }
}