package org.mromichov.guessthesong.core.exception

class UnsupportedPlaylistUrlException : IllegalArgumentException()

class TrackPreviewNotFoundException(private val searchQuery: String) : Exception() {
    override val message: String
        get() = "Track preview not found, cause: $searchQuery"
}

class TrackCountException(
    val actualTrackCount: Int,
    val neededTrackCount: Int,
) : Exception() {
    override val message: String
        get() = "There are $actualTrackCount tracks in the playlist. Needed: $neededTrackCount"
}

class TrackPreviewCountException(
    val foundTrackPreviewCount: Int,
    val neededTrackCount: Int,
) : Exception() {
    override val message: String
        get() = "There are found $foundTrackPreviewCount track previews. Needed: $neededTrackCount"
}

class ItunesAccessForbiddenException : Exception() {
    override val message: String
        get() = "Failed to load preview. Possible cause: VPN or proxy is turned on."
}
