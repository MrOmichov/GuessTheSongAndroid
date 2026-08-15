package org.mromichov.guessthesong.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientCreator {
    fun create() = HttpClient {
        install(Logging) {
            level = LogLevel.ALL
        }

        val jsonConfig = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }

        install(ContentNegotiation) {
            json(jsonConfig, contentType = ContentType.Application.Json)
            json(jsonConfig, contentType = ContentType.parse("text/javascript"))
            json(jsonConfig, contentType = ContentType.parse("text/plain"))
        }
    }
}