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
import com.ntg.lmd.order.domain.model.OrderHistoryUi
import com.ntg.lmd.utils.timeHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SimpleDateFormat")
fun exportOrdersHistoryPdf(
    context: Context,
    orders: List<OrderHistoryUi>,
): Uri? {
    if (orders.isEmpty()) return null

    val doc = PdfDocument()
    val style = PdfStyle(context)
    val config = PdfConfig(context)

    var y = config.startY
    var pageIndex = 1
    var currentPage = newPage(PageContext(doc, config, context, style, pageIndex++, y)) { y = it }

    orders.forEach { order ->
        if (y + config.lineH > config.pageHeight - config.margin) {
            doc.finishPage(currentPage)
            currentPage = newPage(PageContext(doc, config, context, style, pageIndex++, config.startY)) { y = it }
        }
        y = drawRow(RowContext(currentPage, config, style.paint, context, order, y))
    }

    doc.finishPage(currentPage)
    val outFile = writePdfToFile(context, doc)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
}

private data class PdfStyle(
    val paint: Paint,
    val bold: Paint,
) {
    constructor(context: Context) : this(
        paint =
            Paint().apply {
                textSize = context.resources.getDimension(R.dimen.pdf_text_size)
                isAntiAlias = true
                color = Color.BLACK
            },
        bold =
            Paint().apply {
                textSize = context.resources.getDimension(R.dimen.pdf_text_size)
                isAntiAlias = true
                color = Color.BLACK
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
    )
}

private data class PdfConfig(
    val margin: Int,
    val lineH: Int,
    val headerSpacing: Int,
    val pageWidth: Int,
    val pageHeight: Int,
    val colCustomer: Int,
    val colTotal: Int,
    val colStatus: Int,
    val colAge: Int,
    val startY: Int,
) {
    constructor(context: Context) : this(
        margin = context.resources.getDimensionPixelSize(R.dimen.pdf_margin),
        lineH = context.resources.getDimensionPixelSize(R.dimen.pdf_line_height),
        headerSpacing = context.resources.getDimensionPixelSize(R.dimen.pdf_header_spacing),
        pageWidth = context.resources.getDimensionPixelSize(R.dimen.pdf_page_width),
        pageHeight = context.resources.getDimensionPixelSize(R.dimen.pdf_page_height),
        colCustomer = context.resources.getDimensionPixelSize(R.dimen.pdf_col_customer),
        colTotal = context.resources.getDimensionPixelSize(R.dimen.pdf_col_total),
        colStatus = context.resources.getDimensionPixelSize(R.dimen.pdf_col_status),
        colAge = context.resources.getDimensionPixelSize(R.dimen.pdf_col_age),
        startY =
            context.resources.getDimensionPixelSize(R.dimen.pdf_margin) +
                context.resources.getDimensionPixelSize(R.dimen.pdf_header_spacing),
    )
}

private data class PageContext(
    val doc: PdfDocument,
    val config: PdfConfig,
    val context: Context,
    val style: PdfStyle,
    val pageIndex: Int,
    val y: Int,
)

private fun newPage(
    ctx: PageContext,
    onNewY: (Int) -> Unit,
): PdfDocument.Page {
    val info = PdfDocument.PageInfo.Builder(ctx.config.pageWidth, ctx.config.pageHeight, ctx.pageIndex).create()
    val page = ctx.doc.startPage(info)
    var yPos = ctx.y

    val c = page.canvas
    yPos = drawTitle(c, ctx, yPos)
    yPos = drawHeaders(c, ctx, yPos)
    yPos = drawDivider(c, ctx, yPos)

    onNewY(yPos)
    return page
}

private fun drawTitle(
    c: android.graphics.Canvas,
    ctx: PageContext,
    y: Int,
): Int {
    c.drawText(ctx.context.getString(R.string.pdf_title), ctx.config.margin.toFloat(), y.toFloat(), ctx.style.bold)
    return y + ctx.config.lineH
}

private fun drawHeaders(
    c: android.graphics.Canvas,
    ctx: PageContext,
    y: Int,
): Int {
    var yPos = y
    c.drawText(ctx.context.getString(R.string.pdf_header_no), ctx.config.margin.toFloat(), yPos.toFloat(), ctx.style.bold)
    c.drawText(
        ctx.context.getString(R.string.pdf_header_customer),
        (ctx.config.margin + ctx.config.colCustomer).toFloat(),
        yPos.toFloat(),
        ctx.style.bold,
    )
    c.drawText(
        ctx.context.getString(R.string.pdf_header_total),
        (ctx.config.pageWidth - ctx.config.colTotal).toFloat(),
        yPos.toFloat(),
        ctx.style.bold,
    )
    c.drawText(
        ctx.context.getString(R.string.pdf_header_status),
        (ctx.config.pageWidth - ctx.config.colStatus).toFloat(),
        yPos.toFloat(),
        ctx.style.bold,
    )
    c.drawText(
        ctx.context.getString(R.string.pdf_header_age),
        (ctx.config.pageWidth - ctx.config.colAge).toFloat(),
        yPos.toFloat(),
        ctx.style.bold,
    )
    return yPos + ctx.config.lineH
}

private fun drawDivider(
    c: android.graphics.Canvas,
    ctx: PageContext,
    y: Int,
): Int {
    c.drawLine(
        ctx.config.margin.toFloat(),
        y.toFloat(),
        (ctx.config.pageWidth - ctx.config.margin).toFloat(),
        y.toFloat(),
        ctx.style.paint,
    )
    return y + ctx.config.headerSpacing
}

private data class RowContext(
    val page: PdfDocument.Page,
    val config: PdfConfig,
    val paint: Paint,
    val context: Context,
    val order: OrderHistoryUi,
    val y: Int,
)

private fun drawRow(ctx: RowContext): Int {
    val c = ctx.page.canvas
    val y = ctx.y

    c.drawText(ctx.order.number, ctx.config.margin.toFloat(), y.toFloat(), ctx.paint)
    c.drawText(ctx.order.customer, (ctx.config.margin + ctx.config.colCustomer).toFloat(), y.toFloat(), ctx.paint)
    c.drawText(
        String.format(Locale.ROOT, "%.2f", ctx.order.total),
        (ctx.config.pageWidth - ctx.config.colTotal).toFloat(),
        y.toFloat(),
        ctx.paint,
    )
    c.drawText(
        ctx.order.status.name
            .lowercase()
            .replaceFirstChar { it.titlecase() },
        (ctx.config.pageWidth - ctx.config.colStatus).toFloat(),
        y.toFloat(),
        ctx.paint,
    )
    c.drawText(
        timeHelper(ctx.context, ctx.order.createdAtMillis),
        (ctx.config.pageWidth - ctx.config.colAge).toFloat(),
        y.toFloat(),
        ctx.paint,
    )
    return y + ctx.config.lineH
}

private fun writePdfToFile(
    context: Context,
    doc: PdfDocument,
): File {
    val sdf = SimpleDateFormat("yyyyMMdd_HHmm")
    val fileName = "orders_${sdf.format(Date())}.pdf"
    val outFile = File(context.cacheDir, fileName)
    FileOutputStream(outFile).use { doc.writeTo(it) }
    doc.close()
    return outFile
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
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.pdf_share_title)))
}
