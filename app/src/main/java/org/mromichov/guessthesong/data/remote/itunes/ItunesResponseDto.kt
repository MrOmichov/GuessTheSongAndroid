package org.mromichov.guessthesong.data.remote.itunes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItunesTrackDto(
    @SerialName("trackId") val trackId: Long? = null,
    @SerialName("artistName") val artistName: String? = null,
    @SerialName("trackName") val trackName: String? = null,
    @SerialName("previewUrl") val previewUrl: String? = null,
    @SerialName("artworkUrl100") val artworkUrl100: String? = null
)

@Serializable
data class ItunesResponseDto(
    @SerialName("resultCount") val resultCount: Int = 0,
    @SerialName("results") val results: List<ItunesTrackDto> = emptyList()
)
