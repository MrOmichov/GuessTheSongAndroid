package org.mromichov.guessthesong.presentation.error

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** A localized resource reference for text rendered by the UI. */
sealed interface UiText {
    data class Resource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Resource -> stringResource(resId, *args.toTypedArray())
}
