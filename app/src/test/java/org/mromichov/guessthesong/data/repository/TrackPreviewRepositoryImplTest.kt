package org.mromichov.guessthesong.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mromichov.guessthesong.core.database.entity.TrackEntity
import org.mromichov.guessthesong.data.dao.FakeTrackDao
import org.mromichov.guessthesong.data.dao.TrackDao
import org.mromichov.guessthesong.data.remote.itunes.ItunesApi
import org.mromichov.guessthesong.data.remote.itunes.ItunesMapper

class TrackPreviewRepositoryImplTest {

    private val jsonConfig = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    // Вспомогательный метод для создания репозитория в тестах.
    // Позволяет передать свой TrackDao (например, FakeTrackDao) для проверки состояния базы.
    private fun createRepository(
        mockEngine: MockEngine,
        fakeTrackDao: TrackDao = FakeTrackDao(),
    ): TrackPreviewRepositoryImpl {
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
        }
        val api = ItunesApi(client)
        val mapper = ItunesMapper()
        return TrackPreviewRepositoryImpl(
            trackDao = fakeTrackDao,
            itunesApi = api,
            mapper = mapper,
        )
    }

    /**
     * Сценарий 1: Трек уже есть в базе данных (Cache Hit).
     * Ожидаем:
     * - Возвращается трек из БД.
     * - Сетевой запрос вообще НЕ выполняется (requestHistory пустой).
     */
    @Test
    fun getPreview_whenFoundInCache_returnsCachedTrackWithoutCallingNetwork() = runTest {
        // 1. Arrange (Подготовка): заранее кладем трек в FakeTrackDao
        val fakeDao = FakeTrackDao().apply {
            tracks.add(
                TrackEntity(
                    title = "In the End",
                    artist = "Linkin Park",
                    trackPreviewUrl = "https://cached.storage/in_the_end.m4a"
                )
            )
        }

        // Если сетевой движок будет вызван по ошибке, вернем 500
        val mockEngine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError
            )
        }

        val repository = createRepository(mockEngine, fakeTrackDao = fakeDao)

        // 2. Act (Действие): запрашиваем трек
        val result = repository.getPreview(artist = "Linkin Park", title = "In the End")

        // 3. Assert (Проверка):
        assertTrue("Результат должен быть успешным", result.isSuccess)
        val preview = result.getOrNull()
        assertEquals("In the End", preview?.trackName)
        assertEquals("Linkin Park", preview?.artistName)
        assertEquals("https://cached.storage/in_the_end.m4a", preview?.previewUrl)

        // Главная проверка кэша: сеть НЕ вызывалась!
        assertTrue("Сетевой запрос не должен был выполняться при наличии трека в БД", mockEngine.requestHistory.isEmpty())
    }

    /**
     * Сценарий 2: Трека нет в базе (Cache Miss) -> запрашиваем из сети и кэшируем.
     * Ожидаем:
     * - Трек скачивается из iTunes.
     * - Трек автоматически сохраняется в FakeTrackDao.
     */
    @Test
    fun getPreview_whenCacheMiss_fetchesFromNetworkAndSavesToCache() = runTest {
        // 1. Arrange (Подготовка): пустой DAO и успешный ответ от iTunes
        val fakeDao = FakeTrackDao()

        val mockJson = """
        {
            "resultCount": 1,
            "results": [
                {
                    "trackId": 555,
                    "artistName": "Queen",
                    "trackName": "Bohemian Rhapsody",
                    "previewUrl": "https://audio-ssl.itunes.apple.com/bohemian.m4a",
                    "artworkUrl100": "https://is1-ssl.mzstatic.com/queen.jpg"
                }
            ]
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repo = createRepository(mockEngine, fakeTrackDao = fakeDao)

        // 2. Act (Действие): запрашиваем трек
        val result = repo.getPreview("Queen", "Bohemian Rhapsody")

        // 3. Assert (Проверка):
        assertTrue("Результат должен быть успешным", result.isSuccess)
        val preview = result.getOrNull()
        assertEquals("Queen", preview?.artistName)
        assertEquals("Bohemian Rhapsody", preview?.trackName)
        assertEquals("https://audio-ssl.itunes.apple.com/bohemian.m4a", preview?.previewUrl)

        // Проверяем, что трек реально сохранился в базу данных
        assertEquals("В базе должен появиться 1 сохраненный трек", 1, fakeDao.tracks.size)
        val savedTrack = fakeDao.tracks.first()
        assertEquals("Queen", savedTrack.artist)
        assertEquals("Bohemian Rhapsody", savedTrack.title)
        assertEquals("https://audio-ssl.itunes.apple.com/bohemian.m4a", savedTrack.trackPreviewUrl)
    }

    /**
     * Сценарий 3: Трека нет в базе и нет в iTunes.
     * Ожидаем:
     * - Result.failure с NoSuchElementException.
     * - База данных остается пустой.
     */
    @Test
    fun getPreview_notFound_returnsFailure() = runTest {
        val fakeDao = FakeTrackDao()

        val mockJson = """
        {
            "resultCount": 0,
            "results": []
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repo = createRepository(mockEngine, fakeTrackDao = fakeDao)
        val result = repo.getPreview("UnknownArtist", "NonExistentTrack")

        assertTrue("Ожидался Result.failure", result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
        assertTrue("База должна остаться пустой", fakeDao.tracks.isEmpty())
    }

    /**
     * Сценарий 4: Ошибка при записи в БД не должна ломать результат из сети.
     * Репозиторий обернул вставку в runCatching, поэтому даже если БД кинет ошибку,
     * пользователь все равно должен получить свой трек.
     */
    @Test
    fun getPreview_whenDatabaseInsertFails_stillReturnsSuccessFromNetwork() = runTest {
        // Создаем проблемный DAO, который падает при любой вставке
        val failingDao = object : TrackDao {
            override suspend fun insertTracks(vararg track: TrackEntity) {
                throw RuntimeException("Database disk full or SQLite error")
            }

            override suspend fun findByTitleAndArtist(title: String, artist: String): TrackEntity? = null
        }

        val mockJson = """
        {
            "resultCount": 1,
            "results": [
                {
                    "trackId": 777,
                    "artistName": "Linkin Park",
                    "trackName": "Numb",
                    "previewUrl": "https://audio-ssl.itunes.apple.com/numb.m4a"
                }
            ]
        }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val repo = createRepository(mockEngine, fakeTrackDao = failingDao)
        val result = repo.getPreview("Linkin Park", "Numb")

        assertTrue("Метод должен вернуть успех, даже если кэширование в БД упало", result.isSuccess)
        assertEquals("Numb", result.getOrNull()?.trackName)
    }
}
