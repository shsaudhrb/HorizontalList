package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntg.lmd.mainscreen.domain.model.ActiveUser
import com.ntg.lmd.mainscreen.ui.viewmodel.AgentsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReassignBottomSheet(
    open: Boolean,
    state: AgentsState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (ActiveUser) -> Unit
) {
    if (!open) return
    ModalBottomSheet(onDismissRequest = onDismiss) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = state.error, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
            else -> {
                Column(Modifier.padding(12.dp)) {
                    Text("Reassign to…", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(12.dp))
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        items(
                            items = state.agents,                // ← List<ActiveUser>
                            key = { it.id }                       // ← needs items import
                        ) { user ->
                            val disabled = user.id == state.currentUserId
                            ListItem(
                                headlineContent = { Text(user.name) },
                                supportingContent = {
                                    if (disabled) Text("(You)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (!disabled)
                                            Modifier
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable { onSelect(user) }
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
