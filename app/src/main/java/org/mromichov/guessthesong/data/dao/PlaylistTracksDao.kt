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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(trackEntities: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrackCrossRefs(crossRefs: List<PlaylistTrackCrossRef>)

    @Transaction
    suspend fun insertPlaylistWithTracks(playlistWithTracks: PlaylistWithTracks): Long {
        val playlistId = insertPlaylist(playlistWithTracks.playlist)

        val tracksToInsert = playlistWithTracks.tracks.map { track ->
            track.copy(title = track.title, artist = track.artist)
        }
        insertTracks(tracksToInsert)

        val crossRefsToInsert = playlistWithTracks.tracks.map { track ->
            PlaylistTrackCrossRef(playlistId = playlistId, title = track.title, artist = track.artist)
        }
        insertPlaylistTrackCrossRefs(crossRefsToInsert)
        return playlistId
    }

    @Query("SELECT track_preview_url FROM tracks WHERE title= :title AND artist= :artist LIMIT 1")
    suspend fun findTrackPreviewUrlByTitleAndArtist(title: String, artist: String): String?

    @Query("UPDATE tracks SET track_preview_url = :previewUrl WHERE title = :title AND artist = :artist")
    suspend fun updateTrackPreviewUrlByTitleAndArtist(
        title: String,
        artist: String,
        previewUrl: String,
    )

    @Query("SELECT * FROM playlists ORDER BY id")
    suspend fun getPlaylistEntities(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistEntity(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun findPlaylistEntityByTitle(title: String): PlaylistEntity?

    @Query("SELECT tracks.* FROM tracks INNER JOIN playlist_tracks ON tracks.title = playlist_tracks.track_title AND tracks.artist = playlist_tracks.track_artist WHERE playlist_tracks.playlist_id = :id")
    suspend fun getTracksForPlaylist(id: Long): List<TrackEntity>

    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :playlistId")
    suspend fun deletePlaylistCrossRefs(playlistId: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistEntity(id: Long)

    @Query("UPDATE playlists SET title = :title WHERE id = :id")
    suspend fun updatePlaylistTitle(id: Long, title: String)

    @Query("UPDATE playlists SET title = :title, coverFileName = :coverFileName, provider = :provider WHERE id = :id")
    suspend fun updatePlaylistMetadata(id: Long, title: String, coverFileName: String?, provider: String?): Int

    @Query("SELECT coverFileName FROM playlists WHERE coverFileName IS NOT NULL")
    suspend fun getCoverFileNames(): List<String>

    // Explicit reads avoid a Room 3.0.2 composite-Junction code-generation failure.
    @Transaction
    suspend fun getSavedPlaylists(): List<PlaylistWithTracks> =
        getPlaylistEntities().map { PlaylistWithTracks(it, getTracksForPlaylist(it.id)) }

    @Transaction
    suspend fun getSavedPlaylist(id: Long): PlaylistWithTracks? {
        val playlist = getPlaylistEntity(id) ?: return null
        return PlaylistWithTracks(playlist, getTracksForPlaylist(id))
    }

    @Transaction
    suspend fun findSavedPlaylistByTitle(title: String): PlaylistWithTracks? {
        val playlist = findPlaylistEntityByTitle(title) ?: return null
        return PlaylistWithTracks(playlist, getTracksForPlaylist(playlist.id))
    }

    @Transaction
    suspend fun replacePlaylistWithTracks(playlistWithTracks: PlaylistWithTracks) {
        val playlistId = playlistWithTracks.playlist.id
        val playlist = playlistWithTracks.playlist
        check(updatePlaylistMetadata(playlistId, playlist.title, playlist.coverFileName, playlist.provider) == 1) {
            "No saved playlist found for id: $playlistId"
        }
        deletePlaylistCrossRefs(playlistId)
        val tracksToInsert = playlistWithTracks.tracks.map { track ->
            track.copy(title = track.title, artist = track.artist)
        }
        insertTracks(tracksToInsert)
        val crossRefsToInsert = playlistWithTracks.tracks.map { track ->
            PlaylistTrackCrossRef(playlistId = playlistId, title = track.title, artist = track.artist)
        }
        insertPlaylistTrackCrossRefs(crossRefsToInsert)
    }

    @Transaction
    suspend fun deletePlaylistWithTracks(id: Long) {
        deletePlaylistCrossRefs(id)
        deletePlaylistEntity(id)
    }
}
