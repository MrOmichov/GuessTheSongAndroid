package org.mromichov.guessthesong.data.local

/** Used by tests that exercise playlist lookup without cover persistence. */
object NoOpPlaylistCoverStorage : PlaylistCoverStorage {
    override suspend fun save(url: String): String? = null
    override suspend fun delete(fileName: String) = Unit
    override suspend fun reconcile(referencedFileNames: Set<String>) = Unit
}
