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
            Text(text = "Плейлист $playlistTitle уже сохранён")
        },
        text = {
            Text(text = "Заменить сохранённый плейлист новым составом или добавить отдельную копию?")
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(onClick = onReplace) {
                    Text(text = "Заменить плейлист")
                }
                Button(onClick = onAdd) {
                    Text("Добавить копию")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Отмена")
            }
        },
    )
}
