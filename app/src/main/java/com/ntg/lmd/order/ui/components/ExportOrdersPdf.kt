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
import com.ntg.lmd.order.domain.model.OrderUi
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date


@SuppressLint("SimpleDateFormat")
fun exportOrdersPdf(context: Context, orders: List<OrderUi>): Uri? {
    if (orders.isEmpty()) return null

    val doc = PdfDocument()
    val paint = Paint().apply {
        textSize = 12f
        isAntiAlias = true
        color = Color.BLACK
    }
    val bold = Paint(paint).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val margin = 40
    val lineH = 24
    val pageWidth = 595
    val pageHeight = 842

    var y = margin + 10
    var pageIndex = 1
    fun newPage(): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex++).create()
        val page = doc.startPage(info)
        y = margin + 10

        val c = page.canvas
        c.drawText("Order History", margin.toFloat(), y.toFloat(), bold)
        y += lineH
        c.drawText("No.", margin.toFloat(), y.toFloat(), bold)
        c.drawText("Customer", (margin + 70).toFloat(), y.toFloat(), bold)
        c.drawText("Total", (pageWidth - 180).toFloat(), y.toFloat(), bold)
        c.drawText("Status", (pageWidth - 110).toFloat(), y.toFloat(), bold)
        c.drawText("Age", (pageWidth - 50).toFloat(), y.toFloat(), bold)
        y += lineH
        c.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), paint)
        y += 10
        return page
    }

    var page = newPage()
    val canvas = { page.canvas }

    orders.forEach { o ->
        if (y + lineH > pageHeight - margin) {
            doc.finishPage(page)
            page = newPage()
        }
        canvas().apply {
            drawText(o.number, margin.toFloat(), y.toFloat(), paint)
            drawText(o.customer, (margin + 70).toFloat(), y.toFloat(), paint)
            drawText(String.format("%.2f", o.total), (pageWidth - 180).toFloat(), y.toFloat(), paint)
            drawText(o.status.name.lowercase().replaceFirstChar { it.uppercase() }, (pageWidth - 110).toFloat(), y.toFloat(), paint)
            drawText(timeHelper(o.createdAtMillis), (pageWidth - 50).toFloat(), y.toFloat(), paint)
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

fun sharePdf(context: Context, pdfUri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share orders PDF"))
}
