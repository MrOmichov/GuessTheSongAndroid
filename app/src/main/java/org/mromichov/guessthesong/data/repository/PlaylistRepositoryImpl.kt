package org.mromichov.guessthesong.data.repository

import kotlinx.coroutines.CancellationException
import org.mromichov.guessthesong.core.database.PlaylistWithTracks
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.TrackEntity
import org.mromichov.guessthesong.data.dao.PlaylistTracksDao
import org.mromichov.guessthesong.data.remote.spotify.SpotifyApi
import org.mromichov.guessthesong.data.remote.spotify.SpotifyMapper
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistTarget
import org.mromichov.guessthesong.data.remote.spotify.SpotifyPlaylistUrlParser
import org.mromichov.guessthesong.data.remote.yandex.YandexApi
import org.mromichov.guessthesong.data.remote.yandex.YandexMapper
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistTarget
import org.mromichov.guessthesong.data.remote.yandex.YandexPlaylistUrlParser
import org.mromichov.guessthesong.domain.model.Playlist
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.repository.PlaylistRepository
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val yandexApi: YandexApi,
    private val yandexUrlParser: YandexPlaylistUrlParser,
    private val yandexMapper: YandexMapper,
    private val spotifyApi: SpotifyApi,
    private val spotifyUrlParser: SpotifyPlaylistUrlParser,
    private val spotifyMapper: SpotifyMapper,
    private val playlistTracksDao: PlaylistTracksDao,
) : PlaylistRepository {

    override suspend fun getPlaylistByUrl(url: String): Result<Playlist> {
        return try {
            val playlist = fetchPlaylistByUrl(url)
            playlistTracksDao.insertPlaylistWithTracks(
                PlaylistWithTracks(
                    playlist = PlaylistEntity(title = playlist.title),
                    tracks = playlist.tracks.map { track ->
                        TrackEntity(title = track.title, artist = track.artist)
                    },
                ),
            )
            Result.success(playlist)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    override suspend fun getSavedPlaylists(): Result<List<Playlist>> = readSaved {
        playlistTracksDao.getSavedPlaylists().map(::toDomain)
    }

    override suspend fun getSavedPlaylist(id: Long): Result<Playlist> = readSaved {
        val savedPlaylist = playlistTracksDao.getSavedPlaylist(id)
            ?: throw NoSuchElementException("No saved playlist found for id: $id")
        toDomain(savedPlaylist)
    }

    private suspend fun fetchPlaylistByUrl(url: String): Playlist {
        spotifyUrlParser.parse(url)?.let { target ->
            when (target) {
                is SpotifyPlaylistTarget.Playlist -> {
                    val response = spotifyApi.getPlaylist(playlistId = target.id)
                    return spotifyMapper.toDomain(playlistId = target.id, dto = response)
                }
            }
        }

        return when (val target = yandexUrlParser.parse(url)) {
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

    private suspend fun <T> readSaved(read: suspend () -> T): Result<T> {
        return try {
            Result.success(read())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    private fun toDomain(savedPlaylist: PlaylistWithTracks): Playlist = Playlist(
        id = savedPlaylist.playlist.id.toString(),
        title = savedPlaylist.playlist.title,
        tracks = savedPlaylist.tracks.map { track ->
            Track(
                title = track.title,
                artist = track.artist,
                coverUrl = null,
                durationMs = 0L,
            )
        },
    )
}
