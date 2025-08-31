package com.ntg.lmd.mainscreen.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ntg.lmd.mainscreen.domain.model.DeliveryLog
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun detailsCell(
    log: DeliveryLog,
    modifier: Modifier = Modifier,
) {
    val formattedDate = formatOrderDate(log.orderDate)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formattedDate,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            log.orderId,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private fun formatOrderDate(raw: String): String =
    try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        val date = input.parse(raw.removeSuffix("Z"))
        val output = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        date?.let { output.format(it) } ?: raw
    } catch (e: ParseException) {
        Log.w("DateFormat", "Failed to parse date: $raw", e)
        raw
    }
