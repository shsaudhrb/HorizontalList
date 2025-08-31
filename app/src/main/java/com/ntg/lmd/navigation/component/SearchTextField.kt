package com.ntg.lmd.navigation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.ntg.lmd.R
import com.ntg.lmd.mainscreen.domain.model.SearchController

@Composable
fun searchTextField(
    search: SearchController,
    placeholder: String,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
) {
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    TextField(
        value = search.text.value,
        onValueChange = search.onTextChange,
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = onPrimary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        trailingIcon = {
            IconButton(onClick = {
                if (search.text.value.isNotEmpty()) {
                    search.onTextChange("")
                } else {
                    search.onToggle(false)
                    focusManager.clearFocus()
                }
            }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.clear_or_close),
                    tint = onPrimary,
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    search.onSubmit(search.text.value)
                    focusManager.clearFocus()
                },
            ),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Transparent,
                unfocusedIndicatorColor = Transparent,
                disabledIndicatorColor = Transparent,
                errorIndicatorColor = Transparent,
                focusedContainerColor = Transparent,
                unfocusedContainerColor = Transparent,
                disabledContainerColor = Transparent,
                errorContainerColor = Transparent,
                cursorColor = onPrimary,
                focusedTextColor = onPrimary,
                unfocusedTextColor = onPrimary,
            ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = onPrimary),
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
    )
}
