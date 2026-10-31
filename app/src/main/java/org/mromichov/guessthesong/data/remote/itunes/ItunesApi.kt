package org.mromichov.guessthesong.data.remote.itunes

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import javax.inject.Inject

class ItunesApi @Inject constructor(private val client: HttpClient) {

    suspend fun searchTrackPreview(query: String, limit: Int = 5): ItunesResponseDto =
        client.get("https://itunes.apple.com/search") {
            expectSuccess = true
            parameter("term", query)
            parameter("media", "music")
            parameter("entity", "song")
            parameter("limit", limit.toString())
        }.body()
}