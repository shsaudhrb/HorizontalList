package com.ntg.lmd.authentication.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ntg.lmd.R
import com.ntg.lmd.authentication.ui.model.InputProps

@Composable
fun usernameField(
    modifier: Modifier = Modifier,
    props: InputProps,
) {
    val hasError = props.showError && props.errorResId != null

    OutlinedTextField(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Username Field" }
                .onFocusChanged { props.onFocusChange(it.isFocused) },
        value = props.value,
        onValueChange = props.onValueChange,
        label = { Text(stringResource(props.label)) },
        placeholder = { Text(stringResource(props.placeholder)) },
        trailingIcon = { usernameTrailingIcon() },
        isError = hasError,
        singleLine = true,
        supportingText = { usernameSupportingText(hasError, props) },
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.textFieldRoundCorner)),
    )
}

@Composable
private fun usernameTrailingIcon() {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun usernameSupportingText(
    hasError: Boolean,
    props: InputProps,
) {
    if (hasError) {
        Text(
            text = stringResource(props.errorResId!!),
            color = MaterialTheme.colorScheme.error,
        )
    }
}
