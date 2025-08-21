package com.ntg.lmd.authentication.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.ntg.lmd.R
import com.ntg.lmd.authentication.ui.model.InputProps

@Composable
fun passwordField(
    modifier: Modifier = Modifier,
    props: InputProps,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val hasError = props.showError && props.errorResId != null

    OutlinedTextField(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Password Field" }
                .onFocusChanged { props.onFocusChange(it.isFocused) },
        value = props.value,
        onValueChange = props.onValueChange,
        label = { Text(stringResource(props.label)) },
        placeholder = { Text(stringResource(props.placeholder)) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        isError = hasError,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        supportingText = {
            if (hasError) {
                Text(
                    text = stringResource(props.errorResId!!),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        shape = RoundedCornerShape(dimensionResource(R.dimen.textFieldRoundCorner)),
    )
}
