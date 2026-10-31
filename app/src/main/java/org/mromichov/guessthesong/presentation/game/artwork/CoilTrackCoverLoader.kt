package org.mromichov.guessthesong.presentation.game.artwork

import android.content.Context
import coil3.Image
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.size.Scale
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

data class CoilPreparedCover(val image: Image) : PreparedCover

class CoilTrackCoverLoader(
    private val context: Context,
    private val client: HttpClient,
    private val imageLoader: ImageLoader,
) : TrackCoverLoader {
    override suspend fun load(url: String): PreparedCover? {
        // Dedicated client has no logger or HTTP cache. Coil only receives in-memory bytes.
        val bytes = client.prepareGet(url) {
            header(HttpHeaders.CacheControl, "no-store")
        }.execute { response ->
            check(response.status.value in 200..299)
            val bytes = response.bodyAsChannel().readRemaining(5L * 1024 * 1024 + 1).readByteArray()
            check(bytes.size <= 5 * 1024 * 1024) { "Track artwork is too large" }
            bytes
        }
        val result = imageLoader.execute(
            ImageRequest.Builder(context)
                .data(bytes)
                .size(400, 400)
                .precision(Precision.EXACT)
                .scale(Scale.FIT)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build(),
        )
        return (result as? SuccessResult)?.let { CoilPreparedCover(it.image) }
    }
}
