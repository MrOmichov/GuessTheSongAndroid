package org.mromichov.guessthesong.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import org.mromichov.guessthesong.domain.model.TrackEntity

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(vararg track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE title= :title AND artist= :artist LIMIT 1")
    suspend fun findByTitleAndArtist(title: String, artist: String): TrackEntity?
}