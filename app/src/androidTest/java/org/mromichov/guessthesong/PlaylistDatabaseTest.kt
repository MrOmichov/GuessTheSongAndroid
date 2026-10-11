package org.mromichov.guessthesong

import android.content.Context
import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.core.database.PlaylistDatabase
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
                    playlist = PlaylistEntity(title = "Persisted playlist"),
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
        } finally {
            reopenedDatabase.close()
        }
    }
}
