package org.mromichov.guessthesong.core.database

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object PlaylistMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE playlists ADD COLUMN coverFileName TEXT")
            connection.execSQL("ALTER TABLE playlists ADD COLUMN provider TEXT")
        }
    }
}
