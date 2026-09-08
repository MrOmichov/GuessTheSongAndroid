package org.mromichov.guessthesong.core.di.module

import android.content.Context
import androidx.room3.Room
import dagger.Module
import dagger.Provides
import org.mromichov.guessthesong.core.database.TrackDatabase
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Provides
    @Singleton
    fun provideTrackDatabase(context: Context): TrackDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TrackDatabase::class.java,
            "tracks"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideTrackDao(db: TrackDatabase) = db.trackDao()
}