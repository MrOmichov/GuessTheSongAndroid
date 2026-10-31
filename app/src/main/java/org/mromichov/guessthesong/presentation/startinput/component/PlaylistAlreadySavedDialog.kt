package org.mromichov.guessthesong.presentation.startinput.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import org.mromichov.guessthesong.R

@Composable
fun PlaylistAlreadySavedDialog(
    playlistTitle: String,
    onDismissRequest: () -> Unit,
    onReplace: () -> Unit,
    onAdd: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.playlist_saved_title, playlistTitle))
        },
        text = {
            Text(text = stringResource(R.string.playlist_conflict_text))
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = onReplace) {
                    Text(text = stringResource(R.string.replace_playlist))
                }
                Button(onClick = onAdd) {
                    Text(stringResource(R.string.add_copy))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
