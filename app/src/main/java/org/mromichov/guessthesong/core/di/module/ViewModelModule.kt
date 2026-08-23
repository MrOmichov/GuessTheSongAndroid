package org.mromichov.guessthesong.core.di.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.mromichov.guessthesong.core.di.ViewModelFactory
import org.mromichov.guessthesong.core.di.ViewModelKey
import org.mromichov.guessthesong.presentation.game.GameViewModel
import org.mromichov.guessthesong.presentation.startinput.StartInputViewModel

@Module
interface ViewModelModule {
    @Binds
    fun bindViewModelFactory(
        factory: ViewModelFactory
    ): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(StartInputViewModel::class)
    fun bindStartInputViewModel(
        viewModel: StartInputViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GameViewModel::class)
    fun bindGameViewModel(
        viewModel: GameViewModel
    ): ViewModel
}