package org.mromichov.guessthesong

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.core.database.PlaylistDatabase
import org.mromichov.guessthesong.core.database.PlaylistMigrations
import org.mromichov.guessthesong.core.database.PlaylistWithTracks
import org.mromichov.guessthesong.core.database.entity.PlaylistEntity
import org.mromichov.guessthesong.core.database.entity.TrackEntity

@RunWith(AndroidJUnit4::class)
class PlaylistDatabaseTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "playlist-persistence-${System.nanoTime()}.db"

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun playlistAndTrackRelations_surviveClosingAndReopeningDatabase() = runBlocking {
        val firstDatabase = Room.databaseBuilder(
            context,
            PlaylistDatabase::class.java,
            databaseName,
        ).build()

        val playlistId = try {
            firstDatabase.playlistTracksDao().insertPlaylistWithTracks(
                PlaylistWithTracks(
                    playlist = PlaylistEntity(
                        title = "Persisted playlist",
                        coverFileName = "persisted-cover.img",
                        provider = "YANDEX_MUSIC",
                    ),
                    tracks = listOf(TrackEntity(title = "Persisted song", artist = "Persisted artist")),
                ),
            )
            firstDatabase.playlistTracksDao().getSavedPlaylists().single().playlist.id
        } finally {
            firstDatabase.close()
        }

        val reopenedDatabase = Room.databaseBuilder(
            context,
            PlaylistDatabase::class.java,
            databaseName,
        ).build()
        try {
            val saved = reopenedDatabase.playlistTracksDao().getSavedPlaylist(playlistId)

            assertEquals("Persisted playlist", saved?.playlist?.title)
            assertEquals(1, saved?.tracks?.size)
            assertEquals("Persisted song", saved?.tracks?.single()?.title)
            assertEquals("Persisted artist", saved?.tracks?.single()?.artist)
            assertEquals("persisted-cover.img", saved?.playlist?.coverFileName)
            assertEquals("YANDEX_MUSIC", saved?.playlist?.provider)
        } finally {
            reopenedDatabase.close()
        }
    }

    @Test
    fun migrationFromVersionOne_preservesPlaylistTracksAndPreviewAndAddsNullMetadata() = runBlocking {
        createVersionOneDatabase()

        val database = Room.databaseBuilder(
            context,
            PlaylistDatabase::class.java,
            databaseName,
        ).addMigrations(PlaylistMigrations.MIGRATION_1_2).build()
        try {
            val saved = database.playlistTracksDao().getSavedPlaylist(41L)

            assertEquals("Legacy playlist", saved?.playlist?.title)
            assertEquals(null, saved?.playlist?.coverFileName)
            assertEquals(null, saved?.playlist?.provider)
            assertEquals("Legacy song", saved?.tracks?.single()?.title)
            assertEquals("https://preview.test/legacy.mp3", saved?.tracks?.single()?.trackPreviewUrl)
            assertEquals(listOf<String>(), database.playlistTracksDao().getCoverFileNames())
        } finally {
            database.close()
        }
    }

    @Test
    fun replacementPersistsCoverAndProviderAndCanClearCover() = runBlocking {
        val database = Room.databaseBuilder(
            context,
            PlaylistDatabase::class.java,
            databaseName,
        ).addMigrations(PlaylistMigrations.MIGRATION_1_2).build()
        try {
            val dao = database.playlistTracksDao()
            val id = dao.insertPlaylistWithTracks(
                PlaylistWithTracks(
                    playlist = PlaylistEntity(title = "Before"),
                    tracks = listOf(TrackEntity(title = "Old", artist = "Artist")),
                ),
            )

            dao.replacePlaylistWithTracks(
                PlaylistWithTracks(
                    playlist = PlaylistEntity(
                        id = id,
                        title = "After",
                        coverFileName = "offline-cover.img",
                        provider = "SPOTIFY",
                    ),
                    tracks = listOf(TrackEntity(title = "New", artist = "Artist")),
                ),
            )
            val replaced = dao.getSavedPlaylist(id)
            assertEquals("offline-cover.img", replaced?.playlist?.coverFileName)
            assertEquals("SPOTIFY", replaced?.playlist?.provider)
            assertEquals("New", replaced?.tracks?.single()?.title)

            dao.replacePlaylistWithTracks(
                PlaylistWithTracks(
                    playlist = PlaylistEntity(id = id, title = "No cover", coverFileName = null, provider = "YANDEX_MUSIC"),
                    tracks = emptyList(),
                ),
            )
            val cleared = dao.getSavedPlaylist(id)
            assertEquals(null, cleared?.playlist?.coverFileName)
            assertEquals("YANDEX_MUSIC", cleared?.playlist?.provider)
            assertEquals(emptyList<String>(), dao.getCoverFileNames())

            try {
                dao.replacePlaylistWithTracks(
                    PlaylistWithTracks(
                        playlist = PlaylistEntity(id = id + 1000, title = "Missing target"),
                        tracks = emptyList(),
                    ),
                )
                throw AssertionError("Replacing a missing playlist must fail")
            } catch (_: IllegalStateException) {
                assertEquals(null, dao.getSavedPlaylist(id + 1000))
            }
        } finally {
            database.close()
        }
    }

    private fun createVersionOneDatabase() {
        val path = context.getDatabasePath(databaseName)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS playlists (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS tracks (title TEXT NOT NULL COLLATE NOCASE, artist TEXT NOT NULL COLLATE NOCASE, track_preview_url TEXT, PRIMARY KEY(title, artist))")
            db.execSQL("CREATE TABLE IF NOT EXISTS playlist_tracks (playlist_id INTEGER NOT NULL, track_title TEXT NOT NULL, track_artist TEXT NOT NULL, PRIMARY KEY(playlist_id, track_title, track_artist), FOREIGN KEY(playlist_id) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(track_title, track_artist) REFERENCES tracks(title, artist) ON UPDATE NO ACTION ON DELETE NO ACTION)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_tracks_track_title_track_artist ON playlist_tracks (track_title, track_artist)")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'e52806b500c960c25a553fc3eb5b9c3d')")
            db.execSQL("INSERT INTO playlists (id, title) VALUES (41, 'Legacy playlist')")
            db.execSQL("INSERT INTO tracks (title, artist, track_preview_url) VALUES ('Legacy song', 'Legacy artist', 'https://preview.test/legacy.mp3')")
            db.execSQL("INSERT INTO playlist_tracks (playlist_id, track_title, track_artist) VALUES (41, 'Legacy song', 'Legacy artist')")
            db.execSQL("PRAGMA user_version = 1")
        }
    }
}
