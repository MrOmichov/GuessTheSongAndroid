package org.mromichov.guessthesong.core.di.module

import dagger.Module
import dagger.Provides
import android.content.Context
import coil3.ImageLoader
import org.mromichov.guessthesong.presentation.game.artwork.CoilTrackCoverLoader
import org.mromichov.guessthesong.presentation.game.artwork.TrackCoverLoader
import android.graphics.BitmapFactory
import java.io.File
import org.mromichov.guessthesong.data.local.FilePlaylistCoverStorage
import org.mromichov.guessthesong.data.local.PlaylistCoverStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.mromichov.guessthesong.core.network.HttpClientCreator
import javax.inject.Singleton

@Module
class NetworkModule {
    @Provides
    @Singleton
    fun provideTrackCoverLoader(context: Context): TrackCoverLoader =
        CoilTrackCoverLoader(
            context.applicationContext,
            HttpClient(OkHttp),
            ImageLoader.Builder(context.applicationContext).diskCache(null).build(),
        )

    @Provides
    @Singleton
    fun providePlaylistCoverStorage(context: Context): PlaylistCoverStorage =
        // The API client's ALL body logger would buffer image responses before our size limit.
        FilePlaylistCoverStorage(File(context.filesDir, FilePlaylistCoverStorage.DIRECTORY_NAME), HttpClient(OkHttp)) { file ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096 ||
                bounds.outWidth.toLong() * bounds.outHeight > 16_777_216L
            ) {
                false
            } else {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                (bitmap != null).also { bitmap?.recycle() }
            }
        }

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClientCreator.create()
    }
}
