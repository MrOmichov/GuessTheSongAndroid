package org.mromichov.guessthesong.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["id", "title", "artist"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["title", "artist"],
            childColumns = ["track_title", "track_artist"],
        )
    ]
)
data class PlaylistTracksCrossRef(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "track_title")
    val title: String,

    @ColumnInfo(name = "track_artist")
    val artist: String,
)
