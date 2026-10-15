package org.mromichov.guessthesong.data.local

/** Files are owned by individual saved playlists, never shared by URL. */
interface PlaylistCoverStorage {
    suspend fun save(url: String): String?
    suspend fun delete(fileName: String)
    suspend fun reconcile(referencedFileNames: Set<String>)
}
