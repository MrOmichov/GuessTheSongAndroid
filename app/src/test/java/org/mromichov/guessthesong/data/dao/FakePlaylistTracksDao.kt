package org.mromichov.guessthesong.data.dao

import org.mromichov.guessthesong.core.database.PlaylistWithTracks
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.PlaylistTrackCrossRef
import org.mromichov.guessthesong.core.database.entity.TrackEntity

/** Small in-memory DAO fake for repository tests. */
class FakePlaylistTracksDao : PlaylistTracksDao {
    private val playlists = linkedMapOf<Long, PlaylistWithTracks>()
    private val tracks = linkedMapOf<Pair<String, String>, TrackEntity>()
    private var nextPlaylistId = 1L

    var failPreviewUpdate: Boolean = false

    override suspend fun insertPlaylist(playlist: PlaylistEntity): Long = nextPlaylistId++

    override suspend fun insertTracks(trackEntities: List<TrackEntity>) {
        trackEntities.forEach { tracks[key(it.title, it.artist)] = it }
    }

    override suspend fun insertPlaylistTrackCrossRefs(crossRefs: List<PlaylistTrackCrossRef>) = Unit

    override suspend fun insertPlaylistWithTracks(playlistWithTracks: PlaylistWithTracks) {
        val id = if (playlistWithTracks.playlist.id == 0L) nextPlaylistId++ else playlistWithTracks.playlist.id
        val playlist = playlistWithTracks.playlist.copy(id = id)
        playlistWithTracks.tracks.forEach { track -> tracks[key(track.title, track.artist)] = track }
        playlists[id] = PlaylistWithTracks(playlist, playlistWithTracks.tracks)
    }

    override suspend fun getSavedPlaylists(): List<PlaylistWithTracks> = playlists.values.toList()

    override suspend fun getSavedPlaylist(id: Long): PlaylistWithTracks? = playlists[id]

    override suspend fun getPlaylistEntities(): List<PlaylistEntity> = playlists.values.map { it.playlist }

    override suspend fun getPlaylistEntity(id: Long): PlaylistEntity? = playlists[id]?.playlist

    override suspend fun getTracksForPlaylist(id: Long): List<TrackEntity> = playlists[id]?.tracks.orEmpty()

    override suspend fun findTrackPreviewUrlByTitleAndArtist(title: String, artist: String): String? =
        tracks[key(title, artist)]?.trackPreviewUrl

    override suspend fun updateTrackPreviewUrlByTitleAndArtist(
        title: String,
        artist: String,
        previewUrl: String,
    ) {
        if (failPreviewUpdate) error("simulated preview cache write failure")
        val trackKey = key(title, artist)
        tracks[trackKey]?.let { tracks[trackKey] = it.copy(trackPreviewUrl = previewUrl) }
    }

    fun seedTrack(track: TrackEntity) {
        tracks[key(track.title, track.artist)] = track
    }

    private fun key(title: String, artist: String) = title.lowercase() to artist.lowercase()
}
