package com.ntg.lmd.order.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.ntg.lmd.R
import com.ntg.lmd.order.domain.model.OrderUi
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SimpleDateFormat")
fun exportOrdersPdf(
    context: Context,
    orders: List<OrderUi>,
): Uri? {
    if (orders.isEmpty()) return null

    val res = context.resources
    val doc = PdfDocument()
    val paint =
        Paint().apply {
            textSize = res.getDimension(R.dimen.pdf_text_size)
            isAntiAlias = true
            color = Color.BLACK
        }
    val bold =
        Paint(paint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

    // dimens
    val margin = res.getDimensionPixelSize(R.dimen.pdf_margin)
    val lineH = res.getDimensionPixelSize(R.dimen.pdf_line_height)
    val headerSpacing = res.getDimensionPixelSize(R.dimen.pdf_header_spacing)
    val pageWidth = res.getDimensionPixelSize(R.dimen.pdf_page_width)
    val pageHeight = res.getDimensionPixelSize(R.dimen.pdf_page_height)

    val colCustomer = res.getDimensionPixelSize(R.dimen.pdf_col_customer)
    val colTotal = res.getDimensionPixelSize(R.dimen.pdf_col_total)
    val colStatus = res.getDimensionPixelSize(R.dimen.pdf_col_status)
    val colAge = res.getDimensionPixelSize(R.dimen.pdf_col_age)

    var y = margin + headerSpacing
    var pageIndex = 1

    fun newPage(): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
        val page = doc.startPage(info)
        y = margin + headerSpacing

        val c = page.canvas
        c.drawText(context.getString(R.string.pdf_title), margin.toFloat(), y.toFloat(), bold)
        y += lineH

        c.drawText(context.getString(R.string.pdf_header_no), margin.toFloat(), y.toFloat(), bold)
        c.drawText(context.getString(R.string.pdf_header_customer), (margin + colCustomer).toFloat(), y.toFloat(), bold)
        c.drawText(context.getString(R.string.pdf_header_total), (pageWidth - colTotal).toFloat(), y.toFloat(), bold)
        c.drawText(context.getString(R.string.pdf_header_status), (pageWidth - colStatus).toFloat(), y.toFloat(), bold)
        c.drawText(context.getString(R.string.pdf_header_age), (pageWidth - colAge).toFloat(), y.toFloat(), bold)

        y += lineH
        c.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paint)
        y += headerSpacing
        return page
    }

    var page = newPage()

    orders.forEach { o ->
        if (y + lineH > pageHeight - margin) {
            doc.finishPage(page)
            page = newPage()
        }
        page.canvas.apply {
            drawText(o.number, margin.toFloat(), y.toFloat(), paint)
            drawText(o.customer, (margin + colCustomer).toFloat(), y.toFloat(), paint)
            drawText(
                String.format(Locale.ROOT, "%.2f", o.total),
                (pageWidth - colTotal).toFloat(),
                y.toFloat(),
                paint,
            )
            drawText(
                o.status.name
                    .lowercase()
                    .replaceFirstChar { it.titlecase() },
                (pageWidth - colStatus).toFloat(),
                y.toFloat(),
                paint,
            )
            drawText(timeHelper(context, o.createdAtMillis), (pageWidth - colAge).toFloat(), y.toFloat(), paint)
        }
        y += lineH
    }

    doc.finishPage(page)

    val sdf = SimpleDateFormat("yyyyMMdd_HHmm")
    val fileName = "orders_${sdf.format(Date())}.pdf"
    val outFile = File(context.cacheDir, fileName)
    FileOutputStream(outFile).use { doc.writeTo(it) }
    doc.close()

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
}

fun sharePdf(
    context: Context,
    pdfUri: Uri,
) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.pdf_share_title)),
    )
}
