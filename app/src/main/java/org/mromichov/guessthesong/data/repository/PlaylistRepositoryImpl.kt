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
import org.mromichov.guessthesong.domain.model.LocalPlaylist
import org.mromichov.guessthesong.domain.model.PlaylistContent
import org.mromichov.guessthesong.domain.model.RemotePlaylist
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

    override suspend fun fetchPlaylistByUrl(url: String): Result<RemotePlaylist> {
        return try {
            Result.success(fetchPlaylistByUrlInternal(url))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    override suspend fun getPlaylistByUrl(url: String): Result<RemotePlaylist> {
        return try {
            val playlist = fetchPlaylistByUrlInternal(url)
            savePlaylist(playlist.content).getOrThrow()
            Result.success(playlist)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    override suspend fun getLocalPlaylists(): Result<List<LocalPlaylist>> = readSaved {
        playlistTracksDao.getSavedPlaylists().map(::toDomain)
    }

    override suspend fun getLocalPlaylist(id: Long): Result<LocalPlaylist> = readSaved {
        val savedPlaylist = playlistTracksDao.getSavedPlaylist(id)
            ?: throw NoSuchElementException("No saved playlist found for id: $id")
        toDomain(savedPlaylist)
    }

    override suspend fun findLocalPlaylistByTitle(title: String): Result<LocalPlaylist?> = readSaved {
        playlistTracksDao.findSavedPlaylistByTitle(title)?.let(::toDomain)
    }

    override suspend fun savePlaylist(playlistContent: PlaylistContent): Result<LocalPlaylist> = writeSaved {
        val playlistWithTracks = PlaylistWithTracks(
            playlist = PlaylistEntity(title = playlistContent.title),
            tracks = playlistContent.tracks.map { track ->
                TrackEntity(title = track.title, artist = track.artist)
            },
        )
        val generatedId = playlistTracksDao.insertPlaylistWithTracks(playlistWithTracks)
        LocalPlaylist(id = generatedId, content = playlistContent)
    }

    override suspend fun replacePlaylist(existingId: Long, playlistContent: PlaylistContent): Result<LocalPlaylist> =
        writeSaved {
            val playlistWithTracks = PlaylistWithTracks(
                playlist = PlaylistEntity(id = existingId, title = playlistContent.title),
                tracks = playlistContent.tracks.map { track ->
                    TrackEntity(title = track.title, artist = track.artist)
                },
            )
            playlistTracksDao.replacePlaylistWithTracks(playlistWithTracks)
            LocalPlaylist(id = existingId, content = playlistContent)
        }

    override suspend fun deletePlaylist(id: Long): Result<Unit> = writeSaved {
        playlistTracksDao.deletePlaylistWithTracks(id)
    }

    private suspend fun fetchPlaylistByUrlInternal(url: String): RemotePlaylist {
        spotifyUrlParser.parse(url)?.let { target ->
            when (target) {
                is SpotifyPlaylistTarget.Playlist -> {
                    val response = spotifyApi.getPlaylist(playlistId = target.id)
                    return spotifyMapper.toDomain(playlistExternalId = target.id, dto = response)
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

    private suspend fun <T> readSaved(read: suspend () -> T): Result<T> = writeSaved(read)

    private suspend fun <T> writeSaved(action: suspend () -> T): Result<T> {
        return try {
            Result.success(action())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    private fun toDomain(localPlaylistWithTracks: PlaylistWithTracks): LocalPlaylist = LocalPlaylist(
        id = localPlaylistWithTracks.playlist.id,
        content = PlaylistContent(
            title = localPlaylistWithTracks.playlist.title,
            tracks = localPlaylistWithTracks.tracks.map { track ->
                Track(
                    title = track.title,
                    artist = track.artist,
                    coverUrl = null,
                    durationMs = 0L,
                )
            },
        ),
    )
}
