package org.mromichov.guessthesong.data.repository

import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistTarget
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val yandexApi: YandexApi,
    private val urlParser: YandexPlaylistUrlParser,
    private val mapper: YandexMapper
) : PlaylistRepository {

    override suspend fun getPlaylistByUrl(url: String): Result<Playlist> = runCatching {
        when (val target = urlParser.parse(url)) {
            is YandexPlaylistTarget.UserPlaylist -> {
                val response = yandexApi.getUserPlaylist(owner = target.owner, kind = target.kind)
                mapper.toDomain(playlistId = "${target.owner}:${target.kind}", dto = response)
            }
            is YandexPlaylistTarget.UuidPlaylist -> {
                val response = yandexApi.getUuidPlaylist(uuid = target.uuid)
                mapper.toDomain(playlistId = target.uuid, dto = response)
            }
            null -> throw IllegalArgumentException("Unsupported or invalid Yandex Music playlist URL: $url")
        }
    }
}
