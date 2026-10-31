package org.mromichov.guessthesong.presentation

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mromichov.guessthesong.R
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationResourcesTest {
    @Test
    fun russianAndEnglishResourcesCoverMainFlowAndUserErrors() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val russian = localizedContext(appContext, Locale("ru"))
        val english = localizedContext(appContext, Locale.ENGLISH)

        assertEquals("Загрузить плейлист", russian.getString(R.string.import_playlist))
        assertEquals("Игра окончена", russian.getString(R.string.game_over))
        assertEquals("Что-то пошло не так. Попробуйте ещё раз.", russian.getString(R.string.error_unknown))

        assertEquals("Import playlist", english.getString(R.string.import_playlist))
        assertEquals("Game over", english.getString(R.string.game_over))
        assertEquals("Something went wrong. Please try again.", english.getString(R.string.error_unknown))
    }

    private fun localizedContext(context: android.content.Context, locale: Locale) =
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(locale) },
        )
}
