package org.mromichov.guessthesong.data.repository

import org.mromichov.guessthesong.data.dao.TrackDao
import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.data.remote.itunes.ItunesMapper
import org.mromichov.guessthesong.core.database.entity.TrackEntity
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import javax.inject.Inject

class TrackPreviewRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao,
    private val itunesApi: ItunesApi,
    private val mapper: ItunesMapper,
) : TrackPreviewRepository {

    override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> = runCatching {
        val cachedTrack = tryFindInDB(artist, title)
        if (cachedTrack != null) {
            return@runCatching TrackPreview(
                null,
                cachedTrack.title,
                cachedTrack.artist,
                cachedTrack.trackPreviewUrl,
                artworkUrl = null,
            )
        }
        val query = "$artist $title".trim()
        val response = itunesApi.searchTrackPreview(query = query)
        val preview = mapper.findBestMatch(response)
            ?: throw NoSuchElementException("No audio preview found in iTunes for query: $query")

        runCatching {
            trackDao.insertTracks(
                TrackEntity(
                    title = preview.trackName,
                    artist = preview.artistName,
                    trackPreviewUrl = preview.previewUrl
                )
            )
        }

        preview
    }

    suspend fun tryFindInDB(artist: String, title: String) = trackDao.findByTitleAndArtist(title, artist)
}
