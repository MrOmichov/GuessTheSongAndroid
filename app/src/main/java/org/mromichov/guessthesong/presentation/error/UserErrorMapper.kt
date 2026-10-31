package org.mromichov.guessthesong.presentation.error

import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.core.exception.ItunesAccessForbiddenException
import org.mromichov.guessthesong.core.exception.TrackCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewNotFoundException
import org.mromichov.guessthesong.core.exception.UnsupportedPlaylistUrlException

enum class UserErrorContext {
    IMPORT,
    SELECTION,
    PREPARATION,
    STORAGE,
    SAVE,
    LIBRARY,
    PLAYBACK,
}

object UserErrorMapper {
    fun map(error: Throwable, context: UserErrorContext): UiText = when (error) {
        is ItunesAccessForbiddenException -> UiText.Resource(R.string.error_itunes_access)
        is TrackCountException -> UiText.Resource(
            R.string.error_playlist_too_few_tracks,
            listOf(error.actualTrackCount, error.neededTrackCount),
        )
        is TrackPreviewCountException -> UiText.Resource(
            R.string.error_not_enough_previews,
            listOf(error.foundTrackPreviewCount, error.neededTrackCount),
        )
        is TrackPreviewNotFoundException -> UiText.Resource(R.string.error_preview_not_found)
        is UnsupportedPlaylistUrlException -> UiText.Resource(R.string.error_invalid_playlist_link)
        else -> fallback(context)
    }

    private fun fallback(context: UserErrorContext): UiText.Resource = UiText.Resource(
        when (context) {
            UserErrorContext.IMPORT -> R.string.error_import_failed
            UserErrorContext.SELECTION -> R.string.error_open_playlist
            UserErrorContext.PREPARATION -> R.string.error_prepare_game
            UserErrorContext.STORAGE -> R.string.error_playlist_storage
            UserErrorContext.SAVE -> R.string.error_save_playlist
            UserErrorContext.LIBRARY -> R.string.error_load_library
            UserErrorContext.PLAYBACK -> R.string.error_playback
        },
    )
}
