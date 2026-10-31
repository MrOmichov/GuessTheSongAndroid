package org.mromichov.guessthesong.presentation.sharedcomponent

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import org.mromichov.guessthesong.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopGuessTheSongAppBar(
    back: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = "Guess the Song",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        navigationIcon = {
            if (back != null) {
                IconButton(onClick = back) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
    )
}