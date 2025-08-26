package com.ntg.lmd.navigation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R

data class AppHeaderActions(
    val onBackClick: () -> Unit,
    val onMenuClick: () -> Unit = {},
)

data class AppHeaderSearch(
    val visible: Boolean = false,
    val value: String = "",
    val onValueChange: (String) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appHeader(
    title: String,
    showBack: Boolean,
    actions: AppHeaderActions,
    search: AppHeaderSearch = AppHeaderSearch(),
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = { Text(title, color = MaterialTheme.colorScheme.onPrimary) },
            navigationIcon = {
                IconButton(
                    onClick = { if (showBack) actions.onBackClick() else actions.onMenuClick() },
                ) {
                    Icon(
                        imageVector =
                            if (showBack) {
                                Icons.AutoMirrored.Filled.ArrowBack
                            } else {
                                Icons.Filled.Menu
                            },
                        contentDescription = if (showBack) "Back" else "Menu",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
        )

        if (search.visible) {
            Spacer(Modifier.height(8.dp))
            TextField(
                value = search.value,
                onValueChange = search.onValueChange,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_by_order_number),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                    ),
            )
        }
    }
}
