package org.mromichov.guessthesong.data.repository

import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistTarget
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistTarget
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val yandexApi: YandexApi,
    private val yandexUrlParser: YandexPlaylistUrlParser,
    private val yandexMapper: YandexMapper,
    private val spotifyApi: SpotifyApi,
    private val spotifyUrlParser: SpotifyPlaylistUrlParser,
    private val spotifyMapper: SpotifyMapper
) : PlaylistRepository {

    override suspend fun getPlaylistByUrl(url: String): Result<Playlist> = runCatching {
        spotifyUrlParser.parse(url)?.let { target ->
            when (target) {
                is SpotifyPlaylistTarget.Playlist -> {
                    val response = spotifyApi.getPlaylist(playlistId = target.id)
                        ?: throw NoSuchElementException("Spotify playlist not found: ${target.id}")
                    return@runCatching spotifyMapper.toDomain(response)
                }
            }
        }

        when (val target = yandexUrlParser.parse(url)) {
            is YandexPlaylistTarget.UserPlaylist -> {
                val response = yandexApi.getUserPlaylist(owner = target.owner, kind = target.kind)
                yandexMapper.toDomain(playlistId = "${target.owner}:${target.kind}", dto = response)
            }
            is YandexPlaylistTarget.UuidPlaylist -> {
                val response = yandexApi.getUuidPlaylist(uuid = target.uuid)
                yandexMapper.toDomain(playlistId = target.uuid, dto = response)
            }
            null -> throw IllegalArgumentException("Unsupported or invalid playlist URL: $url")
        }
    }
}
