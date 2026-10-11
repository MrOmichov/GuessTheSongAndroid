package org.mromichov.guessthesong.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity

@Database(entities = [PlaylistEntity::class], version = 1, exportSchema = false)
abstract class PlaylistDatabase : RoomDatabase() {
}