package com.ntg.lmd.mainscreen.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ntg.lmd.R

@Composable
fun deliverDialog( // dialogs used in myOrder Screen
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.deliver_order)) },
        text = {
            Column {
                Text(stringResource(R.string.enter_pin_optional))
                Spacer(Modifier.height(8.dp))
                TextField(value = pin, onValueChange = { pin = it }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.deliver)) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun reasonDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.please_enter_reason))
                Spacer(Modifier.height(8.dp))
                TextField(value = reason, onValueChange = { reason = it }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(reason) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun simpleConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(stringResource(R.string.are_you_sure)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.ok)) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun reassignDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var assignee by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reassign_order)) },
        text = {
            Column {
                Text(stringResource(R.string.enter_agent_name_or_id))
                Spacer(Modifier.height(8.dp))
                TextField(value = assignee, onValueChange = { assignee = it })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(assignee) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
