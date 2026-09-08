package org.mromichov.guessthesong.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(tableName = "tracks", primaryKeys = ["title", "artist"])
data class TrackEntity(
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val title: String,

    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val artist: String,
    val trackPreviewUrl: String,
)
