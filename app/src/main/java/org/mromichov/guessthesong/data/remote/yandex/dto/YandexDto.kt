package org.mromichov.guessthesong.data.remote.yandex.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YandexArtistDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("name") val name: String = ""
)

@Serializable
data class YandexTrackDto(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String = "",
    @SerialName("artists") val artists: List<YandexArtistDto> = emptyList(),
    @SerialName("coverUri") val coverUri: String? = null,
    @SerialName("ogImage") val ogImage: String? = null,
    @SerialName("durationMs") val durationMs: Long = 0L
)

@Serializable
data class YandexWebPlaylistDto(
    @SerialName("title") val title: String? = null,
    @SerialName("tracks") val tracks: List<YandexTrackDto> = emptyList()
)

@Serializable
data class YandexWebPlaylistResponseDto(
    @SerialName("playlist") val playlist: YandexWebPlaylistDto? = null
)

@Serializable
data class YandexApiTrackContainerDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("track") val track: YandexTrackDto? = null
)

@Serializable
data class YandexApiPlaylistDto(
    @SerialName("title") val title: String? = null,
    @SerialName("tracks") val tracks: List<YandexApiTrackContainerDto> = emptyList()
)

@Serializable
data class YandexApiPlaylistResponseDto(
    @SerialName("result") val result: YandexApiPlaylistDto? = null
)
