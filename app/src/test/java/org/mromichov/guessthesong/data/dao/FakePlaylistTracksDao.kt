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
    var failPlaylistWrite: Boolean = false
    var savedPlaylistReadCount: Int = 0

    override suspend fun insertPlaylist(playlist: PlaylistEntity): Long = nextPlaylistId++

    override suspend fun insertTracks(trackEntities: List<TrackEntity>) {
        trackEntities.forEach { tracks[key(it.title, it.artist)] = it }
    }

    override suspend fun insertPlaylistTrackCrossRefs(crossRefs: List<PlaylistTrackCrossRef>) = Unit

    override suspend fun insertPlaylistWithTracks(playlistWithTracks: PlaylistWithTracks): Long {
        if (failPlaylistWrite) error("simulated playlist write failure")
        val id = if (playlistWithTracks.playlist.id == 0L) nextPlaylistId++ else playlistWithTracks.playlist.id
        val playlist = playlistWithTracks.playlist.copy(id = id)
        playlistWithTracks.tracks.forEach { track -> tracks[key(track.title, track.artist)] = track }
        playlists[id] = PlaylistWithTracks(playlist, playlistWithTracks.tracks)
        return id
    }

    override suspend fun findPlaylistEntityByTitle(title: String): PlaylistEntity? =
        playlists.values.firstOrNull { it.playlist.title.equals(title, ignoreCase = true) }?.playlist

    override suspend fun deletePlaylistCrossRefs(playlistId: Long) {
        playlists[playlistId]?.let {
            playlists[playlistId] = it.copy(tracks = emptyList())
        }
    }

    override suspend fun deletePlaylistEntity(id: Long) {
        playlists.remove(id)
    }

    override suspend fun updatePlaylistTitle(id: Long, title: String) {
        playlists[id]?.let {
            playlists[id] = it.copy(playlist = it.playlist.copy(title = title))
        }
    }

    override suspend fun updatePlaylistMetadata(id: Long, title: String, coverFileName: String?, provider: String?): Int {
        val existing = playlists[id] ?: return 0
        playlists[id] = existing.copy(
            playlist = existing.playlist.copy(
                title = title,
                coverFileName = coverFileName,
                provider = provider,
            ),
        )
        return 1
    }

    override suspend fun findSavedPlaylistByTitle(title: String): PlaylistWithTracks? =
        playlists.values.firstOrNull { it.playlist.title.equals(title, ignoreCase = true) }

    override suspend fun replacePlaylistWithTracks(playlistWithTracks: PlaylistWithTracks) {
        if (failPlaylistWrite) error("simulated playlist write failure")
        val id = playlistWithTracks.playlist.id
        playlistWithTracks.tracks.forEach { track ->
            val trackKey = key(track.title, track.artist)
            if (!tracks.containsKey(trackKey)) {
                tracks[trackKey] = track
            }
        }
        playlists[id] = PlaylistWithTracks(playlistWithTracks.playlist, playlistWithTracks.tracks)
    }

    override suspend fun deletePlaylistWithTracks(id: Long) {
        playlists.remove(id)
    }

    override suspend fun getSavedPlaylists(): List<PlaylistWithTracks> {
        savedPlaylistReadCount++
        return playlists.values.toList()
    }

    override suspend fun getSavedPlaylist(id: Long): PlaylistWithTracks? = playlists[id]

    override suspend fun getPlaylistEntities(): List<PlaylistEntity> = playlists.values.map { it.playlist }

    override suspend fun getCoverFileNames(): List<String> =
        playlists.values.mapNotNull { it.playlist.coverFileName }

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
