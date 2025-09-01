package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntg.lmd.mainscreen.domain.model.ActiveUser
import com.ntg.lmd.mainscreen.ui.viewmodel.AgentsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun reassignBottomSheet(
    open: Boolean,
    state: AgentsState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (ActiveUser) -> Unit,
) {
    if (!open) return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        when {
            state.isLoading -> ReassignLoading()
            state.error != null -> ReassignError(error = state.error, onRetry = onRetry)
            else -> ReassignList(state = state, onSelect = onSelect)
        }
    }
}

@Composable
private fun ReassignLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReassignError(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = error, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ReassignList(
    state: AgentsState,
    onSelect: (ActiveUser) -> Unit,
) {
    Column(Modifier.padding(12.dp)) {
        Text(
            text = "Reassign to…",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(12.dp),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            items(
                items = state.agents,
                key = { it.id },
            ) { user ->
                ReassignUserItem(
                    user = user,
                    isCurrent = user.id == state.currentUserId,
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun ReassignUserItem(
    user: ActiveUser,
    isCurrent: Boolean,
    onSelect: (ActiveUser) -> Unit,
) {
    val clickableMod =
        if (!isCurrent) {
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onSelect(user) }
        } else {
            Modifier
        }

    ListItem(
        headlineContent = { Text(user.name) },
        supportingContent = {
            if (isCurrent) {
                Text("(You)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableMod),
    )
}
