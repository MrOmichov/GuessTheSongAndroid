package org.mromichov.guessthesong.presentation.error

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mromichov.guessthesong.R
import org.mromichov.guessthesong.core.exception.ItunesAccessForbiddenException
import org.mromichov.guessthesong.core.exception.TrackCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewCountException
import org.mromichov.guessthesong.core.exception.TrackPreviewNotFoundException
import org.mromichov.guessthesong.core.exception.UnsupportedPlaylistUrlException

class UserErrorMapperTest {
    @Test
    fun `known track count errors map to localized resources with counts`() {
        assertEquals(
            UiText.Resource(R.string.error_playlist_too_few_tracks, listOf(3, 10)),
            UserErrorMapper.map(TrackCountException(3, 10), UserErrorContext.PREPARATION),
        )
        assertEquals(
            UiText.Resource(R.string.error_not_enough_previews, listOf(2, 5)),
            UserErrorMapper.map(TrackPreviewCountException(2, 5), UserErrorContext.PREPARATION),
        )
    }

    @Test
    fun `preview errors do not expose technical details`() {
        assertEquals(
            UiText.Resource(R.string.error_itunes_access),
            UserErrorMapper.map(ItunesAccessForbiddenException(), UserErrorContext.PLAYBACK),
        )
        assertEquals(
            UiText.Resource(R.string.error_preview_not_found),
            UserErrorMapper.map(TrackPreviewNotFoundException("secret query"), UserErrorContext.PREPARATION),
        )
    }

    @Test
    fun `unknown errors use context fallback and discard exception message`() {
        assertEquals(
            UiText.Resource(R.string.error_import_failed),
            UserErrorMapper.map(IllegalStateException("SQL constraint secret"), UserErrorContext.IMPORT),
        )
        assertEquals(
            UiText.Resource(R.string.error_playback),
            UserErrorMapper.map(RuntimeException("internal player state"), UserErrorContext.PLAYBACK),
        )
        assertEquals(
            UiText.Resource(R.string.error_playlist_storage),
            UserErrorMapper.map(IllegalStateException("database details"), UserErrorContext.STORAGE),
        )
    }

    @Test
    fun `invalid link gets actionable import message`() {
        assertEquals(
            UiText.Resource(R.string.error_invalid_playlist_link),
            UserErrorMapper.map(UnsupportedPlaylistUrlException(), UserErrorContext.IMPORT),
        )
        assertEquals(
            UiText.Resource(R.string.error_import_failed),
            UserErrorMapper.map(IllegalArgumentException("internal parser details"), UserErrorContext.IMPORT),
        )
    }
}
