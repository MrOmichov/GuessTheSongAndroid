package org.mromichov.guessthesong.core.di.module

import android.content.Context
import androidx.room3.Room
import dagger.Module
import dagger.Provides
import org.mromichov.guessthesong.core.database.PlaylistDatabase
import org.mromichov.guessthesong.data.dao.PlaylistTracksDao
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun provideTrackDatabase(context: Context): PlaylistDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            PlaylistDatabase::class.java,
            "tracks"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePlaylistTracksDao(database: PlaylistDatabase): PlaylistTracksDao =
        database.playlistTracksDao()

}
