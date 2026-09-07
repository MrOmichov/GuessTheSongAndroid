package org.mromichov.guessthesong.domain.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val artist: String,
    val trackPreviewUrl: String
)
