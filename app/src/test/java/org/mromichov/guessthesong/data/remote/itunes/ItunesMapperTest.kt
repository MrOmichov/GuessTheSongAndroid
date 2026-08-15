package org.mromichov.guessthesong.data.remote.itunes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ItunesMapperTest {

    private val mapper = ItunesMapper()

    @Test
    fun findBestMatch_withValidPreview_returnsTrackPreview() {
        val response = ItunesResponseDto(
            resultCount = 2,
            results = listOf(
                ItunesTrackDto(
                    trackId = 1L,
                    artistName = "Linkin Park",
                    trackName = "Numb (Karaoke)",
                    previewUrl = null
                ),
                ItunesTrackDto(
                    trackId = 2L,
                    artistName = "Linkin Park",
                    trackName = "Numb",
                    previewUrl = "https://audio-ssl.itunes.apple.com/numb.m4a",
                    artworkUrl100 = "https://is1-ssl.mzstatic.com/cover.jpg"
                )
            )
        )

        val result = mapper.findBestMatch(response)

        assertNotNull(result)
        assertEquals(2L, result?.trackId)
        assertEquals("Linkin Park", result?.artistName)
        assertEquals("Numb", result?.trackName)
        assertEquals("https://audio-ssl.itunes.apple.com/numb.m4a", result?.previewUrl)
        assertEquals("https://is1-ssl.mzstatic.com/cover.jpg", result?.artworkUrl)
    }

    @Test
    fun findBestMatch_emptyResults_returnsNull() {
        val response = ItunesResponseDto(resultCount = 0, results = emptyList())
        assertNull(mapper.findBestMatch(response))
    }

    @Test
    fun findBestMatch_noValidPreviews_returnsNull() {
        val response = ItunesResponseDto(
            resultCount = 1,
            results = listOf(
                ItunesTrackDto(trackId = 1L, previewUrl = "   ")
            )
        )
        assertNull(mapper.findBestMatch(response))
    }
}
