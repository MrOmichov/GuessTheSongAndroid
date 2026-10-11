package org.mromichov.guessthesong.data.dao

import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity

interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    
}