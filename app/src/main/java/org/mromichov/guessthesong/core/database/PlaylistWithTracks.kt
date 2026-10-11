package org.mromichov.guessthesong.core.database

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.PlaylistTrackCrossRef
import org.mromichov.guessthesong.core.database.entity.TrackEntity

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["title", "artist"],
        associateBy = Junction(
            PlaylistTrackCrossRef::class,
            parentColumns = ["playlist_id"],
            entityColumns = ["track_title", "track_artist"],
        )
    )
    val tracks: List<TrackEntity>
)