package org.mromichov.guessthesong.core.di.module

import dagger.Module
import dagger.Provides
import org.mromichov.guessthesong.BuildConfig
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyConfig
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import javax.inject.Singleton

@Module
class SpotifyModule {

    @Provides
    @Singleton
    fun provideSpotifyConfig(): SpotifyConfig {
        return SpotifyConfig(
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
        )
    }

    @Provides
    @Singleton
    fun provideSpotifyApi(config: SpotifyConfig): SpotifyApi {
        return SpotifyApi(config)
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
