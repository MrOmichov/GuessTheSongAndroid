package org.mromichov.guessthesong.presentation.startinput.component

import org.mromichov.guessthesong.R
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry

class PlaylistAlreadySavedDialogTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun showsPlaylistTitleAndRunsReplaceCallback() {
        var replaceCalls = 0
        var addCalls = 0
        var dismissCalls = 0
        showDialog(
            onDismiss = { dismissCalls++ },
            onReplace = { replaceCalls++ },
            onAdd = { addCalls++ },
        )

        composeRule.onNodeWithText(context.getString(R.string.playlist_saved_title, "Favorites")).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.replace_playlist)).performClick()

        composeRule.runOnIdle {
            assertEquals(1, replaceCalls)
            assertEquals(0, addCalls)
            assertEquals(0, dismissCalls)
        }
    }

    @Test
    fun runsAddCopyCallback() {
        var replaceCalls = 0
        var addCalls = 0
        var dismissCalls = 0
        showDialog(
            onDismiss = { dismissCalls++ },
            onReplace = { replaceCalls++ },
            onAdd = { addCalls++ },
        )

        composeRule.onNodeWithText(context.getString(R.string.add_copy)).performClick()

        composeRule.runOnIdle {
            assertEquals(0, replaceCalls)
            assertEquals(1, addCalls)
            assertEquals(0, dismissCalls)
        }
    }

    @Test
    fun runsDismissCallbackFromCancelButton() {
        var replaceCalls = 0
        var addCalls = 0
        var dismissCalls = 0
        showDialog(
            onDismiss = { dismissCalls++ },
            onReplace = { replaceCalls++ },
            onAdd = { addCalls++ },
        )

        composeRule.onNodeWithText(context.getString(R.string.cancel)).performClick()

        composeRule.runOnIdle {
            assertEquals(0, replaceCalls)
            assertEquals(0, addCalls)
            assertEquals(1, dismissCalls)
        }
    }

    private fun showDialog(
        onDismiss: () -> Unit,
        onReplace: () -> Unit,
        onAdd: () -> Unit,
    ) {
        composeRule.setContent {
            PlaylistAlreadySavedDialog(
                playlistTitle = "Favorites",
                onDismissRequest = onDismiss,
                onReplace = onReplace,
                onAdd = onAdd,
            )
        }
    }
}
