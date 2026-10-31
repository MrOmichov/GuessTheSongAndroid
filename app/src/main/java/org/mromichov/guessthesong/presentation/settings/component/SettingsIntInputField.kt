package org.mromichov.guessthesong.presentation.settings.component

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsIntInputField(
    initValue: String,
    changeSetting: (Int) -> String?,
    showSnackbar: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initValue) }
    var validText by rememberSaveable { mutableStateOf(initValue) }
    var isFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = text,
        modifier = Modifier
            .widthIn(max = 80.dp)
            .onFocusChanged { focus ->
                if (isFocused && !focus.isFocused) text = validText
                isFocused = focus.isFocused
            },
        onValueChange = { newValue ->
            text = newValue
            val validatedInput =
                if (newValue.isEmpty()) null
                else newValue.toIntOrNull()

            val error: String? = if (validatedInput == null) {
                "Введите целое число"
            } else {
                changeSetting(validatedInput)
            }
            if (error == null) {
                validText = validatedInput.toString()
            } else {
                showSnackbar(error)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        singleLine = true
    )
}