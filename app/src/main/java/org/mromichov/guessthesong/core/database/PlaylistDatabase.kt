package org.mromichov.guessthesong.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.PlaylistTrackCrossRef
import org.mromichov.guessthesong.core.database.entity.TrackEntity
import org.mromichov.guessthesong.data.dao.PlaylistTracksDao

@Database(
    entities = [PlaylistEntity::class, TrackEntity::class, PlaylistTrackCrossRef::class],
    version = 2,
    exportSchema = true
)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistTracksDao(): PlaylistTracksDao
}
