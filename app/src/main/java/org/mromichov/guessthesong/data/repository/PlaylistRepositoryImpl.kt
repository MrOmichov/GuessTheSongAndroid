package org.mromichov.guessthesong.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mromichov.guessthesong.data.local.PlaylistCoverStorage
import org.mromichov.guessthesong.domain.model.playlist.PlaylistProvider
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
import org.mromichov.guessthesong.domain.model.Track
import org.mromichov.guessthesong.domain.model.playlist.LocalPlaylist
import org.mromichov.guessthesong.domain.model.playlist.PlaylistContent
import org.mromichov.guessthesong.domain.model.playlist.RemotePlaylist
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
    private val coverStorage: PlaylistCoverStorage,
) : PlaylistRepository {
    private val storageMutex = Mutex()
    private var reconciled = false

    override suspend fun fetchPlaylistByUrl(url: String): Result<RemotePlaylist> {
        return try {
            Result.success(fetchPlaylistByUrlInternal(url))
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

    override suspend fun savePlaylist(
        playlistContent: PlaylistContent,
        provider: PlaylistProvider?,
    ): Result<LocalPlaylist> = writeSaved {
        persistPlaylist(null, playlistContent, provider)
    }

    override suspend fun replacePlaylist(
        existingId: Long,
        playlistContent: PlaylistContent,
        provider: PlaylistProvider?,
    ): Result<LocalPlaylist> = writeSaved {
        persistPlaylist(existingId, playlistContent, provider)
    }

    override suspend fun deletePlaylist(id: Long): Result<Unit> = writeSaved {
        val oldFile = playlistTracksDao.getPlaylistEntity(id)?.coverFileName
        currentCoroutineContext().ensureActive()
        // Finish the short DB operation even if cancellation arrives at the commit boundary.
        withContext(NonCancellable) {
            playlistTracksDao.deletePlaylistWithTracks(id)
            deleteUnusedFile(oldFile)
        }
    }

    private suspend fun persistPlaylist(
        existingId: Long?,
        content: PlaylistContent,
        provider: PlaylistProvider?,
    ): LocalPlaylist {
        val old = existingId?.let {
            playlistTracksDao.getPlaylistEntity(it)
                ?: throw NoSuchElementException("No saved playlist found for id: $it")
        }
        val newFile = content.coverUrl?.takeIf { it.isNotBlank() }?.let { url ->
            try {
                coverStorage.save(url)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
        }
        var committed = false
        try {
            currentCoroutineContext().ensureActive()
            return withContext(NonCancellable) {
                val entity = PlaylistEntity(
                    id = existingId ?: 0L,
                    title = content.title,
                    coverFileName = newFile,
                    provider = provider?.name,
                )
                val saved = PlaylistWithTracks(entity, content.tracks.map {
                    TrackEntity(title = it.title, artist = it.artist)
                })
                val id = if (existingId == null) {
                    playlistTracksDao.insertPlaylistWithTracks(saved)
                } else {
                    playlistTracksDao.replacePlaylistWithTracks(saved)
                    existingId
                }
                committed = true
                deleteUnusedFile(old?.coverFileName)
                LocalPlaylist(id, content, newFile, provider)
            }
        } finally {
            if (!committed) withContext(NonCancellable) { deleteUnusedFile(newFile) }
        }
    }

    /** A cleanup failure must not turn a committed write into an apparent rollback. */
    private suspend fun deleteUnusedFile(fileName: String?) {
        if (fileName == null) return
        try {
            coverStorage.delete(fileName)
        } catch (_: Exception) {
            reconciled = false
        }
    }

    private suspend fun reconcileCovers() {
        if (reconciled) return
        val references = playlistTracksDao.getCoverFileNames().toSet()
        try {
            coverStorage.reconcile(references)
            reconciled = true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Retry next access; saved playlists remain readable.
        }
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
            storageMutex.withLock {
                reconcileCovers()
                Result.success(action())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    private fun toDomain(localPlaylistWithTracks: PlaylistWithTracks): LocalPlaylist = LocalPlaylist(
        id = localPlaylistWithTracks.playlist.id,
        coverFileName = localPlaylistWithTracks.playlist.coverFileName,
        provider = PlaylistProvider.entries.firstOrNull { it.name == localPlaylistWithTracks.playlist.provider },
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
