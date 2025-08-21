package com.ntg.lmd.navigation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R
import com.ntg.lmd.ui.theme.DeepRed
import com.ntg.lmd.ui.theme.MediumGray
import com.ntg.lmd.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appHeader(
    title: String,
    onMenuClick: () -> Unit,
    showSearch: Boolean,
    searchValue: String,
    onSearchChange: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .background(DeepRed)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // First row: title + menu
        androidx.compose.material3.TopAppBar(
            title = { Text(title, color = White) },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = null, tint = White)
                }
            },
            actions = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DeepRed),
            colors =
                androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepRed,
                    titleContentColor = White,
                    navigationIconContentColor = White,
                ),
        )

        if (showSearch) {
            Spacer(Modifier.height(8.dp))
            TextField(
                value = searchValue,
                onValueChange = onSearchChange,
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = MediumGray) },
                placeholder = { Text(stringResource(R.string.search_by_order_number), color = MediumGray) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = DeepRed,
                    ),
            )
        }
    }
}
