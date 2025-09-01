package com.ntg.lmd.notification.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.ntg.lmd.R
import com.ntg.lmd.notification.ui.model.NotificationUi

fun LazyListScope.footerAppendState(lazyPagingItems: LazyPagingItems<NotificationUi>) {
    when (val state = lazyPagingItems.loadState.append) {
        is LoadState.Loading -> footerLoading()
        is LoadState.Error ->
            footerError(
                message = state.error.message,
                onRetry = { lazyPagingItems.retry() },
            )

        is LoadState.NotLoading -> if (state.endOfPaginationReached) footerEndReached()
    }
}

private fun LazyListScope.footerLoading() {
    item {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.mediumSpace)),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
    }
}

private fun LazyListScope.footerError(
    message: String?,
    onRetry: () -> Unit,
) {
    item {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.mediumSpace)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message ?: stringResource(R.string.loading_more_failed),
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.smallerSpace)))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

private fun LazyListScope.footerEndReached() {
    item {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.largerSpace)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.caught_up),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
