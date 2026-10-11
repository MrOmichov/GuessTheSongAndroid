package org.mromichov.guessthesong.core.di.module

import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import javax.inject.Singleton

@Module
class SpotifyModule {

    @Provides
    @Singleton
    fun provideSpotifyApi(client: HttpClient): SpotifyApi {
        return SpotifyApi(client)
    }

    @Provides
    @Singleton
    fun provideSpotifyPlaylistUrlParser(): SpotifyPlaylistUrlParser {
        return SpotifyPlaylistUrlParser()
    }

    @Provides
    @Singleton
    fun provideSpotifyMapper(): SpotifyMapper {
        return SpotifyMapper()
    }
}
