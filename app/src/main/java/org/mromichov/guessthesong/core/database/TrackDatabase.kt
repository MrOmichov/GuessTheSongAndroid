package org.mromichov.guessthesong.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import org.mromichov.guessthesong.data.dao.TrackDao
import org.mromichov.guessthesong.domain.model.TrackEntity

@Database(entities = [TrackEntity::class], version = 1)
abstract class TrackDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}