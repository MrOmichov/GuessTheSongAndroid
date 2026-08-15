package org.mromichov.guessthesong.core.di

import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import org.mromichov.guessthesong.core.network.HttpClientCreator
import javax.inject.Singleton

@Module
class NetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClientCreator.create()
    }
}