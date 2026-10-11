package org.mromichov.guessthesong.data.remote.spotify.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyWebEmbedDto(
    @SerialName("props")
    val props: SpotifyPropsDto? = null
)

@Serializable
data class SpotifyPropsDto(
    @SerialName("pageProps")
    val pageProps: SpotifyPagePropsDto? = null
)

@Serializable
data class SpotifyPagePropsDto(
    @SerialName("state")
    val state: SpotifyStateDto? = null
)

@Serializable
data class SpotifyStateDto(
    @SerialName("data")
    val data: SpotifyDataDto? = null
)

@Serializable
data class SpotifyDataDto(
    @SerialName("entity")
    val entity: SpotifyEntityDto? = null
)

@Serializable
data class SpotifyEntityDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("coverArt")
    val coverArt: SpotifyCoverArtDto? = null,
    @SerialName("trackList")
    val trackList: List<SpotifyWebTrackDto> = emptyList()
)

@Serializable
data class SpotifyCoverArtDto(
    @SerialName("sources")
    val sources: List<SpotifyImageSourceDto> = emptyList()
)

@Serializable
data class SpotifyImageSourceDto(
    @SerialName("url")
    val url: String? = null,
    @SerialName("height")
    val height: Int? = null,
    @SerialName("width")
    val width: Int? = null
)

@Serializable
data class SpotifyWebTrackDto(
    @SerialName("uri")
    val uri: String? = null,
    @SerialName("uid")
    val uid: String? = null,
    @SerialName("title")
    val title: String,
    @SerialName("subtitle")
    val subtitle: String? = null,
    @SerialName("duration")
    val duration: Long = 0,
    @SerialName("audioPreview")
    val audioPreview: SpotifyAudioPreviewDto? = null
)

@Serializable
data class SpotifyAudioPreviewDto(
    @SerialName("format")
    val format: String? = null,
    @SerialName("url")
    val url: String? = null
)
