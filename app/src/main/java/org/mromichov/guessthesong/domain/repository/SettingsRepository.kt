package org.mromichov.guessthesong.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val minimalRoundsNumber: Flow<Int>
    val snippetLength: Flow<Long>

    suspend fun setMinimalRoundsNumber(minimalRoundsNumber: Int)
    suspend fun setSnippetLength(ms: Long)
}