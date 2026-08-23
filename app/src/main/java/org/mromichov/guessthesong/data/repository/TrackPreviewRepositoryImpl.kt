package org.mromichov.guessthesong.data.repository

import android.util.Log
import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.data.remote.itunes.ItunesMapper
import org.mromichov.guessthesong.domain.model.TrackPreview
import org.mromichov.guessthesong.domain.repository.TrackPreviewRepository
import javax.inject.Inject

class TrackPreviewRepositoryImpl @Inject constructor(
    private val itunesApi: ItunesApi,
    private val mapper: ItunesMapper
) : TrackPreviewRepository {

    override suspend fun getPreview(artist: String, title: String): Result<TrackPreview> = runCatching {
        val query = "$artist $title".trim()
        val response = itunesApi.searchTrackPreview(query = query)
        Log.d("preview",  if (response.results[0].previewUrl != null) response.results[0].previewUrl!! else "Pizda")
        mapper.findBestMatch(response)
            ?: throw NoSuchElementException("No audio preview found in iTunes for query: $query")
    }
}
