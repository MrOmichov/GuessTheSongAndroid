package org.mromichov.guessthesong.data.dao

import org.mromichov.guessthesong.core.database.entity.TrackEntity

class FakeTrackDao : TrackDao {
    val tracks = mutableListOf<TrackEntity>()

    override suspend fun insertTracks(vararg track: TrackEntity) {
        tracks.addAll(track)
    }

    override suspend fun findByTitleAndArtist(title: String, artist: String): TrackEntity? {
        return tracks.firstOrNull {
            it.title.equals(title, ignoreCase = true) &&
                    it.artist.equals(artist, ignoreCase = true)
        }
    }
}