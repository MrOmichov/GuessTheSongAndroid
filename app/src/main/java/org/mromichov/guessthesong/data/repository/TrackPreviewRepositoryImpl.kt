package org.mromichov.guessthesong.data.repository

import kotlinx.coroutines.CancellationException
import org.mromichov.guessthesong.data.dao.PlaylistTracksDao
import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.data.remote.itunes.ItunesMapper
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import javax.inject.Inject

class TrackPreviewRepositoryImpl @Inject constructor(
    private val itunesApi: ItunesApi,
    private val mapper: ItunesMapper,
    private val playlistTracksDao: PlaylistTracksDao,
) : TrackPreviewRepository {

    override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> {
        return try {
            val cachedPreviewUrl = playlistTracksDao
                .findTrackPreviewUrlByTitleAndArtist(title = title, artist = artist)
                ?.takeIf(String::isNotBlank)

            if (cachedPreviewUrl != null) {
                return Result.success(
                    TrackPreview(
                        title = title,
                        artist = artist,
                        trackPreviewUrl = cachedPreviewUrl,
                        coverUrl = null,
                    ),
                )
            }

            val query = "$artist $title".trim()
            val response = itunesApi.searchTrackPreview(query = query)
            val preview = mapper.findBestMatch(response)
                ?: throw NoSuchElementException("No audio preview found in iTunes for query: $query")

            try {
                playlistTracksDao.updateTrackPreviewUrlByTitleAndArtist(
                    title = title,
                    artist = artist,
                    previewUrl = preview.trackPreviewUrl,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // A cache write failure must not discard a preview already found online.
            }

            Result.success(preview)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }
}
