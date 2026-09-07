package org.mromichov.guessthesong.data.repository

import org.mromichov.guessthesong.core.database.TrackDatabase
import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.data.remote.itunes.ItunesMapper
import org.mromichov.guessthesong.domain.model.TrackEntity
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import javax.inject.Inject

class TrackPreviewRepositoryImpl @Inject constructor(
    private val trackDatabase: TrackDatabase,
    private val itunesApi: ItunesApi,
    private val mapper: ItunesMapper,
) : TrackPreviewRepository {

    // TODO доделать
    override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> = runCatching {
        val trackEntity = tryFindInDB(artist, title)
        if (trackEntity != null) {
            return@runCatching TrackPreview(
                trackEntity.id.toLong(),
                trackEntity.title,
                trackEntity.artist,
                trackEntity.trackPreviewUrl,
                artworkUrl = ""
            )
        }
        val query = "$artist $title".trim()
        val response = itunesApi.searchTrackPreview(query = query)
        mapper.findBestMatch(response)
            ?: throw NoSuchElementException("No audio preview found in iTunes for query: $query")
    }

    suspend fun tryFindInDB(artist: String, title: String): TrackEntity? {
        val trackDao = trackDatabase.trackDao()
        return trackDao.findByTitleAndArtist(artist, title)
    }
}
