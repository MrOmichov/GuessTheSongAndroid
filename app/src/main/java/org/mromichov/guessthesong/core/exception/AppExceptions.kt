package org.mromichov.guessthesong.core.exception

class TrackPreviewException : Exception() {
    override val message: String
        get() = "Failed to get preview"
}

class TrackCountException : Exception() {
    override val message: String
        get() = "There are fewer tracks in the playlist than needed"
}