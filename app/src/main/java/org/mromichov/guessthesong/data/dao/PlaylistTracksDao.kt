package org.mromichov.guessthesong.data.dao

import androidx.room3.*
import org.mromichov.guessthesong.core.database.PlaylistWithTracks
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.PlaylistTrackCrossRef
import org.mromichov.guessthesong.core.database.entity.TrackEntity

@Dao
interface PlaylistTracksDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(trackEntities: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrackCrossRefs(crossRefs: List<PlaylistTrackCrossRef>)

    @Transaction
    suspend fun insertPlaylistWithTracks(playlistWithTracks: PlaylistWithTracks) {
        val playlistId = insertPlaylist(playlistWithTracks.playlist)

        val tracksToInsert = playlistWithTracks.tracks.map { track ->
            track.copy(title = track.title, artist = track.artist)
        }
        insertTracks(tracksToInsert)

        val crossRefsToInsert = playlistWithTracks.tracks.map { track ->
            PlaylistTrackCrossRef(playlistId = playlistId, title = track.title, artist = track.artist)
        }
        insertPlaylistTrackCrossRefs(crossRefsToInsert)
    }

    @Query("SELECT track_preview_url FROM tracks WHERE title= :title AND artist= :artist LIMIT 1")
    suspend fun findTrackPreviewUrlByTitleAndArtist(title: String, artist: String): String
}