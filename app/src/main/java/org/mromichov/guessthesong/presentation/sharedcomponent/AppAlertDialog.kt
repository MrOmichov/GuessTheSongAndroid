package org.mromichov.guessthesong.presentation.sharedcomponent

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    dismissButton: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: Painter
) {
    AlertDialog(
        icon = { Icon(painter = icon, contentDescription = "AlertDialog icon") },
        title = { Text(dialogTitle) },
        text = { Text(dialogText) },
        onDismissRequest = onDismissRequest,
        dismissButton = dismissButton,
        confirmButton = confirmButton,
    )
}