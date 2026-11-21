package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.BusinessSettings
import com.example.data.Customer
import com.example.data.Payment
import com.example.data.WalletType
import java.io.File
import java.io.FileOutputStream

object ReceiptService {

    fun renderReceiptBitmap(
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Bitmap {
        val rows = mutableListOf(
            "No. Kuitansi" to payment.receiptNumber,
            "Periode Tagihan" to Formatters.formatYearMonthFull(payment.yearMonth),
            "Tanggal Bayar" to Formatters.formatDateFull(payment.paidDateMillis),
            "Nama Pelanggan" to payment.customerName,
            "Paket Internet" to payment.packageName,
            "Metode Bayar" to payment.wallet.labelId,
        )
        if (payment.wallet == WalletType.BANK_TRANSFER && payment.bankReference.isNotBlank()) {
            rows.add("Referensi Bank" to payment.bankReference.take(28))
        }
        if (!customer?.address.isNullOrBlank()) {
            rows.add("Alamat" to customer!!.address.take(28))
        }

        val width = 840
        val estimatedFooterEnd = 472f + (rows.size * 56f) + 20f + 185f +
            (if (payment.notes.isNotBlank()) 42f else 0f) + 44f + 74f
        val height = maxOf(1240, estimatedFooterEnd.toInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean paper background
        canvas.drawColor(Color.parseColor("#FFFFFF"))

        // Top Emerald Brand Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B6E4F")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 230f, headerPaint)

        // Subtle Gold Accent Bar
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
        }
        canvas.drawRect(0f, 230f, width.toFloat(), 242f, accentPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D5F2E7")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }

        val businessTitle = settings.businessName.ifBlank { "NetKas Wi-Fi" }
        canvas.drawText(businessTitle, width / 2f, 95f, titlePaint)
        if (settings.businessAddress.isNotBlank()) {
            canvas.drawText(settings.businessAddress.take(52), width / 2f, 140f, subtitlePaint)
        }
        if (settings.businessPhone.isNotBlank()) {
            canvas.drawText("Telp / WA: ${settings.businessPhone}", width / 2f, 178f, subtitlePaint)
        }

        val kuitansiLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#132A3E")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("KUITANSI PEMBAYARAN WI-FI", width / 2f, 305f, kuitansiLabelPaint)

        // LUNAS Badge
        val badgeRect = RectF(width / 2f - 125f, 326f, width / 2f + 125f, 382f)
        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D5F2E7")
        }
        canvas.drawRoundRect(badgeRect, 28f, 28f, badgeBg)
        val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B6E4F")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("STATUS: LUNAS", width / 2f, 363f, badgeText)

        // Dashed divider
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        canvas.drawLine(56f, 418f, width - 56f, 418f, dashPaint)

        // Detail rows
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 25f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val maxValueWidth = width - 340f
        var y = 472f
        rows.forEach { (label, rawValue) ->
            canvas.drawText(label, 56f, y, labelPaint)
            val fittedValue = fitTextToWidth(rawValue, valuePaint, maxValueWidth)
            canvas.drawText(fittedValue, width - 56f, y, valuePaint)
            y += 56f
        }

        // Amount Highlight Box
        val amountBoxTop = y + 20f
        val amountBoxRect = RectF(56f, amountBoxTop, width - 56f, amountBoxTop + 135f)
        val amountBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F8F5")
        }
        val amountBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B6E4F")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(amountBoxRect, 20f, 20f, amountBoxBg)
        canvas.drawRoundRect(amountBoxRect, 20f, 20f, amountBoxBorder)

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B6E4F")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val totalValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B6E4F")
            textSize = 48f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("TOTAL PEMBAYARAN DITERIMA", width / 2f, amountBoxTop + 44f, totalLabelPaint)
        canvas.drawText(Formatters.formatRupiah(payment.amount), width / 2f, amountBoxTop + 106f, totalValPaint)

        // Notes & Footer
        var footerY = amountBoxTop + 185f
        if (payment.notes.isNotBlank()) {
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 22f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Catatan: ${payment.notes.take(55)}", width / 2f, footerY, notePaint)
            footerY += 42f
        }

        canvas.drawLine(56f, footerY, width - 56f, footerY, dashPaint)
        footerY += 44f

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Terima kasih atas pembayaran tepat waktu Anda.", width / 2f, footerY, footerPaint)
        canvas.drawText("Simpan kuitansi ini sebagai bukti pembayaran sah.", width / 2f, footerY + 34f, footerPaint)

        return bitmap
    }

    private fun fitTextToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end) + ellipsis
    }

    fun saveReceiptPngToCache(
        context: Context,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Uri {
        val bitmap = renderReceiptBitmap(payment, customer, settings)
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val safeReceipt = payment.receiptNumber.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(dir, "kuitansi_${safeReceipt}.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } finally {
            bitmap.recycle()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun saveReceiptPngToUri(
        context: Context,
        uri: Uri,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ): Result<Unit> = runCatching {
        val bitmap = renderReceiptBitmap(payment, customer, settings)
        try {
            val out = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Tidak dapat menyimpan foto kuitansi ke lokasi tujuan.")
            out.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("Gagal mengompresi foto kuitansi.")
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun printReceipt(
        context: Context,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val bitmap = renderReceiptBitmap(payment, customer, settings)
        val jobName = "Kuitansi_${payment.receiptNumber}"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                val pdfDoc = PdfDocument()
                try {
                    // Compact receipt page size (58mm/80mm friendly aspect or A5 scaled)
                    val pageWidth = 420
                    val pageHeight = 620
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val destRect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                    page.canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                    pdfDoc.finishPage(page)

                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.localizedMessage)
                } finally {
                    pdfDoc.close()
                }
            }
        }
        printManager.print(jobName, adapter, null)
    }

    fun shareReceiptViaWhatsApp(
        context: Context,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ) {
        val uri = saveReceiptPngToCache(context, payment, customer, settings)
        val caption = buildString {
            append("Halo ${payment.customerName},\n")
            append("Berikut kuitansi pembayaran Wi-Fi *${settings.businessName.ifBlank { "NetKas Wi-Fi" }}*:\n")
            append("• Periode: ${Formatters.formatYearMonthFull(payment.yearMonth)}\n")
            append("• Paket: ${payment.packageName}\n")
            append("• Nominal: ${Formatters.formatRupiah(payment.amount)}\n")
            append("• Metode: ${payment.wallet.labelId}\n")
            append("• No. Kuitansi: ${payment.receiptNumber}\n\n")
            append("Terima kasih atas pembayarannya!")
        }

        val pm = context.packageManager
        val waPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }

        val normalizedPhone = Formatters.normalizeIndonesianPhoneForWhatsApp(customer?.phone.orEmpty())

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = android.content.ClipData.newRawUri("Kuitansi", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (waPackage != null) {
                setPackage(waPackage)
                if (normalizedPhone.isNotBlank()) {
                    putExtra("jid", "$normalizedPhone@s.whatsapp.net")
                }
            }
        }
        try {
            context.startActivity(
                if (waPackage != null) shareIntent else Intent.createChooser(shareIntent, "Kirim Foto Kuitansi")
            )
        } catch (_: Exception) {
            shareIntent.setPackage(null)
            shareIntent.removeExtra("jid")
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Foto Kuitansi"))
        }
    }

    fun shareReceiptViaEmail(
        context: Context,
        payment: Payment,
        customer: Customer?,
        settings: BusinessSettings,
    ) {
        val uri = saveReceiptPngToCache(context, payment, customer, settings)
        val subject = "Kuitansi Pembayaran Wi-Fi ${Formatters.formatYearMonthFull(payment.yearMonth)} - ${payment.receiptNumber}"
        val body = buildString {
            append("Yth. ${payment.customerName},\n\n")
            append("Terlampir foto kuitansi pembayaran layanan Wi-Fi ${settings.businessName.ifBlank { "NetKas Wi-Fi" }}.\n")
            append("Periode: ${Formatters.formatYearMonthFull(payment.yearMonth)}\n")
            append("Paket: ${payment.packageName}\n")
            append("Nominal: ${Formatters.formatRupiah(payment.amount)}\n")
            append("Metode: ${payment.wallet.labelId}\n")
            append("Tanggal Bayar: ${Formatters.formatDateFull(payment.paidDateMillis)}\n\n")
            append("Terima kasih.")
        }

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            if (!customer?.email.isNullOrBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(customer!!.email.trim()))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("Kuitansi", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(emailIntent, "Kirim Kuitansi via Email"))
    }

    fun sendWhatsAppDueReminder(
        context: Context,
        customer: Customer,
        yearMonth: String,
        settings: BusinessSettings,
    ) {
        val biz = settings.businessName.ifBlank { "NetKas Wi-Fi" }
        val message = buildString {
            append("Halo ${customer.name},\n")
            append("Pengingat tagihan Wi-Fi *$biz* untuk periode *${Formatters.formatYearMonthFull(yearMonth)}*:\n")
            append("• Paket: ${customer.packageName}\n")
            append("• Tagihan: ${Formatters.formatRupiah(customer.monthlyFee)}\n")
            append("• Jatuh Tempo: Tanggal ${customer.dueDay}\n")
            if (settings.bankAccountInfo.isNotBlank()) {
                append("\nPembayaran transfer dapat dikirim ke:\n${settings.bankAccountInfo}\n")
            }
            append("\nMohon konfirmasi setelah melakukan pembayaran. Terima kasih!")
        }

        val normalizedPhone = Formatters.normalizeIndonesianPhoneForWhatsApp(customer.phone)
        val uri = if (normalizedPhone.isNotBlank()) {
            Uri.parse("https://wa.me/$normalizedPhone?text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://wa.me/?text=${Uri.encode(message)}")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(fallback, "Kirim Pengingat Tagihan"))
        }
    }
}
