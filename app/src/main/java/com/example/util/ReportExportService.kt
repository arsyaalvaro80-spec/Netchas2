package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.BillingStatus
import com.example.data.BusinessSettings
import com.example.data.CustomerBillingItem
import com.example.data.Expense
import com.example.data.MonthlyFinancialSummary
import com.example.data.Payment
import com.example.data.WalletTransfer
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExportService {

    fun exportMonthlyPdfAndShare(
        context: Context,
        yearMonth: String,
        settings: BusinessSettings,
        summary: MonthlyFinancialSummary,
        payments: List<Payment>,
        expenses: List<Expense>,
        transfers: List<WalletTransfer>,
        billingItems: List<CustomerBillingItem>,
    ): Result<File> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Laporan_NetKas_${yearMonth}.pdf")
        FileOutputStream(file).use { out ->
            writeMonthlyPdfToStream(
                outputStream = out,
                yearMonth = yearMonth,
                settings = settings,
                summary = summary,
                payments = payments,
                expenses = expenses,
                transfers = transfers,
                billingItems = billingItems,
            )
        }
        shareExportedFile(context, file, "application/pdf", "Bagikan Laporan PDF")
        file
    }

    fun saveMonthlyPdfToUri(
        context: Context,
        uri: Uri,
        yearMonth: String,
        settings: BusinessSettings,
        summary: MonthlyFinancialSummary,
        payments: List<Payment>,
        expenses: List<Expense>,
        transfers: List<WalletTransfer>,
        billingItems: List<CustomerBillingItem>,
    ): Result<Unit> = runCatching {
        val out = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Tidak dapat membuka file tujuan di penyimpanan perangkat.")
        out.use { stream ->
            writeMonthlyPdfToStream(
                outputStream = stream,
                yearMonth = yearMonth,
                settings = settings,
                summary = summary,
                payments = payments,
                expenses = expenses,
                transfers = transfers,
                billingItems = billingItems,
            )
        }
    }

    fun exportMonthlyExcelAndShare(
        context: Context,
        yearMonth: String,
        settings: BusinessSettings,
        summary: MonthlyFinancialSummary,
        payments: List<Payment>,
        expenses: List<Expense>,
        transfers: List<WalletTransfer>,
        billingItems: List<CustomerBillingItem>,
    ): Result<File> = runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "Laporan_NetKas_${yearMonth}.xlsx")
        FileOutputStream(file).use { out ->
            writeMonthlyExcelToStream(
                outputStream = out,
                yearMonth = yearMonth,
                settings = settings,
                summary = summary,
                payments = payments,
                expenses = expenses,
                transfers = transfers,
                billingItems = billingItems,
            )
        }
        shareExportedFile(
            context,
            file,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "Bagikan Laporan Excel (.xlsx)"
        )
        file
    }

    fun saveMonthlyExcelToUri(
        context: Context,
        uri: Uri,
        yearMonth: String,
        settings: BusinessSettings,
        summary: MonthlyFinancialSummary,
        payments: List<Payment>,
        expenses: List<Expense>,
        transfers: List<WalletTransfer>,
        billingItems: List<CustomerBillingItem>,
    ): Result<Unit> = runCatching {
        val out = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Tidak dapat membuka file tujuan di penyimpanan perangkat.")
        out.use { stream ->
            writeMonthlyExcelToStream(
                outputStream = stream,
                yearMonth = yearMonth,
                settings = settings,
                summary = summary,
                payments = payments,
                expenses = expenses,
                transfers = transfers,
                billingItems = billingItems,
            )
        }
    }

    private fun writeMonthlyPdfToStream(
        outputStream: OutputStream,
        yearMonth: String,
        settings: BusinessSettings,
        summary: MonthlyFinancialSummary,
        payments: List<Payment>,
        expenses: List<Expense>,
        transfers: List<WalletTransfer>,
        billingItems: List<CustomerBillingItem>,
    ) {
        val pdf = PdfDocument()
        try {
            val pageWidth = 595 // A4 points
            val pageHeight = 842
            var pageNumber = 1

            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            var y = 44f

            val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B6E4F") }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D5F2E7")
                textSize = 10.5f
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#132A3E")
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 9.5f
            }
            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1f
            }

            fun ensureSpace(needed: Float) {
                if (y + needed > pageHeight - 40f) {
                    pdf.finishPage(page)
                    pageNumber++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    y = 44f
                }
            }

            // Header Banner
            canvas.drawRoundRect(RectF(32f, 28f, pageWidth - 32f, 102f), 10f, 10f, headerBg)
            canvas.drawText(
                "LAPORAN KEUANGAN & TAGIHAN WI-FI",
                48f, 56f, titlePaint
            )
            canvas.drawText(
                "${settings.businessName.ifBlank { "NetKas Wi-Fi" }}  •  Periode: ${Formatters.formatYearMonthFull(yearMonth)}",
                48f, 76f, subPaint
            )
            canvas.drawText(
                "Dicetak: ${Formatters.formatDateTime(System.currentTimeMillis())}",
                48f, 92f, subPaint
            )
            y = 124f

            // Executive Summary Card
            val boxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F1F8F5") }
            canvas.drawRoundRect(RectF(32f, y, pageWidth - 32f, y + 72f), 8f, 8f, boxBg)
            canvas.drawText("KEUNTUNGAN BERSIH", 48f, y + 22f, sectionPaint)
            val profitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (summary.netProfit >= 0) Color.parseColor("#0B6E4F") else Color.parseColor("#DC2626")
                textSize = 20f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawText(Formatters.formatRupiah(summary.netProfit), 48f, y + 52f, profitPaint)

            canvas.drawText("Total Pemasukan: ${Formatters.formatRupiah(summary.totalIncome)}", pageWidth - 48f, y + 24f, rightPaint)
            canvas.drawText("Total Pengeluaran: ${Formatters.formatRupiah(summary.totalExpense)}", pageWidth - 48f, y + 42f, rightPaint)
            canvas.drawText("Tunggakan (${summary.unpaidCount}): ${Formatters.formatRupiah(summary.unpaidAmount)}", pageWidth - 48f, y + 60f, rightPaint)
            y += 94f

            // Wallet Separation Table (Dompet Tunai vs Transfer Bank)
            canvas.drawText("1. Pemisahan Kas Tunai & Transfer Bank", 32f, y, sectionPaint)
            y += 18f
            canvas.drawText("Dompet", 36f, y, boldPaint)
            canvas.drawText("Pemasukan", 250f, y, boldPaint)
            canvas.drawText("Pengeluaran", 385f, y, boldPaint)
            canvas.drawText("Saldo Bersih", pageWidth - 36f, y, rightPaint)
            y += 6f
            canvas.drawLine(32f, y, pageWidth - 32f, y, linePaint)
            y += 16f

            canvas.drawText("Dompet Tunai (Cash)", 36f, y, textPaint)
            canvas.drawText(Formatters.formatRupiah(summary.cashIncome), 250f, y, textPaint)
            canvas.drawText(Formatters.formatRupiah(summary.cashExpense), 385f, y, textPaint)
            canvas.drawText(Formatters.formatRupiah(summary.netCashBalance), pageWidth - 36f, y, rightPaint)
            y += 18f

            canvas.drawText("Transfer Bank", 36f, y, textPaint)
            canvas.drawText(Formatters.formatRupiah(summary.bankIncome), 250f, y, textPaint)
            canvas.drawText(Formatters.formatRupiah(summary.bankExpense), 385f, y, textPaint)
            canvas.drawText(Formatters.formatRupiah(summary.netBankBalance), pageWidth - 36f, y, rightPaint)
            y += 18f

            if (summary.cashToBankTransfer > 0L || summary.bankToCashTransfer > 0L) {
                canvas.drawText(
                    "Penyesuaian Pindah Saldo: Setor Tunai → Bank ${Formatters.formatRupiah(summary.cashToBankTransfer)}  |  Tarik Bank → Tunai ${Formatters.formatRupiah(summary.bankToCashTransfer)}",
                    36f,
                    y,
                    textPaint
                )
                y += 16f
            }
            canvas.drawText(
                "Akumulasi Total: Dompet Tunai ${Formatters.formatRupiah(summary.cumulativeCashBalance)}  |  Transfer Bank ${Formatters.formatRupiah(summary.cumulativeBankBalance)}",
                36f,
                y,
                boldPaint
            )
            y += 22f

            // Payments List
            ensureSpace(48f)
            canvas.drawText("2. Rincian Pembayaran Pelanggan (${payments.size})", 32f, y, sectionPaint)
            y += 18f
            canvas.drawText("Tanggal", 36f, y, boldPaint)
            canvas.drawText("Pelanggan / Paket", 108f, y, boldPaint)
            canvas.drawText("Metode", 330f, y, boldPaint)
            canvas.drawText("Nominal", pageWidth - 36f, y, rightPaint)
            y += 6f
            canvas.drawLine(32f, y, pageWidth - 32f, y, linePaint)
            y += 15f

            if (payments.isEmpty()) {
                canvas.drawText("Belum ada pembayaran tercatat pada periode ini.", 36f, y, textPaint)
                y += 20f
            } else {
                payments.forEach { p ->
                    ensureSpace(18f)
                    canvas.drawText(Formatters.formatDateFull(p.paidDateMillis), 36f, y, textPaint)
                    canvas.drawText("${p.customerName.take(24)} (${p.packageName.take(14)})", 108f, y, textPaint)
                    canvas.drawText(p.wallet.labelId, 330f, y, textPaint)
                    canvas.drawText(Formatters.formatRupiah(p.amount), pageWidth - 36f, y, rightPaint)
                    y += 16f
                }
            }
            y += 16f

            // Expenses List
            ensureSpace(48f)
            canvas.drawText("3. Rincian Pengeluaran Operasional (${expenses.size})", 32f, y, sectionPaint)
            y += 18f
            canvas.drawText("Tanggal", 36f, y, boldPaint)
            canvas.drawText("Keterangan / Kategori", 108f, y, boldPaint)
            canvas.drawText("Dompet", 350f, y, boldPaint)
            canvas.drawText("Nominal", pageWidth - 36f, y, rightPaint)
            y += 6f
            canvas.drawLine(32f, y, pageWidth - 32f, y, linePaint)
            y += 15f

            if (expenses.isEmpty()) {
                canvas.drawText("Belum ada pengeluaran tercatat pada periode ini.", 36f, y, textPaint)
                y += 20f
            } else {
                expenses.forEach { e ->
                    ensureSpace(18f)
                    canvas.drawText(Formatters.formatDateFull(e.expenseDateMillis), 36f, y, textPaint)
                    canvas.drawText("${e.title.take(26)} • ${e.expenseCategory.labelId}", 108f, y, textPaint)
                    canvas.drawText(e.wallet.labelId, 350f, y, textPaint)
                    canvas.drawText(Formatters.formatRupiah(e.amount), pageWidth - 36f, y, rightPaint)
                    y += 16f
                }
            }
            y += 16f

            // Unpaid Customers List
            val unpaid = billingItems.filter { it.payment == null }
            if (unpaid.isNotEmpty()) {
                ensureSpace(48f)
                canvas.drawText("4. Daftar Pelanggan Belum Bayar (${unpaid.size})", 32f, y, sectionPaint)
                y += 18f
                unpaid.forEach { item ->
                    ensureSpace(18f)
                    canvas.drawText(item.customer.name.take(30), 36f, y, textPaint)
                    canvas.drawText("Jatuh tempo tgl ${item.customer.dueDay}", 260f, y, textPaint)
                    canvas.drawText(Formatters.formatRupiah(item.customer.monthlyFee), pageWidth - 36f, y, rightPaint)
                    y += 16f
                }
                y += 16f
            }

            // Wallet Transfers List
            if (transfers.isNotEmpty()) {
                val secNum = if (unpaid.isNotEmpty()) 5 else 4
                ensureSpace(48f)
                canvas.drawText("$secNum. Pindah Saldo Antar Dompet (${transfers.size})", 32f, y, sectionPaint)
                y += 18f
                transfers.forEach { t ->
                    ensureSpace(18f)
                    canvas.drawText(Formatters.formatDateFull(t.transferDateMillis), 36f, y, textPaint)
                    val desc = "${t.source.labelId} → ${t.destination.labelId}" +
                        if (t.notes.isNotBlank()) " (${t.notes.take(24)})" else ""
                    canvas.drawText(desc, 108f, y, textPaint)
                    canvas.drawText(Formatters.formatRupiah(t.amount), pageWidth - 36f, y, rightPaint)
                    y += 16f
                }
            }

            pdf.finishPage(page)
            pdf.writeTo(outputStream)
        } finally {
            pdf.close()
        }
    }

    private sealed interface ExcelCell {
        data class Text(val value: String) : ExcelCell
        data class Num(val value: Long) : ExcelCell
    }

    private fun t(v: String): ExcelCell = ExcelCell.Text(v)
    private fun n(v: Long): ExcelCell = ExcelCell.Num(v)

    private fun writeMonthlyExcelToStream(
        outputStream: OutputStream,
        yearMonth: String,
        settings: BusinessSettings,
        summary: MonthlyFinancialSummary,
        payments: List<Payment>,
        expenses: List<Expense>,
        transfers: List<WalletTransfer>,
        billingItems: List<CustomerBillingItem>,
    ) {
        val sheet1Rows = listOf(
            listOf(t("Laporan Keuangan NetKas Wi-Fi"), t(settings.businessName.ifBlank { "NetKas Wi-Fi" })),
            listOf(t("Periode"), t(Formatters.formatYearMonthFull(yearMonth))),
            listOf(t("Dicetak"), t(Formatters.formatDateTime(System.currentTimeMillis()))),
            listOf(t(""), t("")),
            listOf(t("RINGKASAN KEUANGAN"), t("NOMINAL (IDR)")),
            listOf(t("Total Pemasukan"), n(summary.totalIncome)),
            listOf(t("Total Pengeluaran"), n(summary.totalExpense)),
            listOf(t("Keuntungan Bersih"), n(summary.netProfit)),
            listOf(t("Total Belum Terbayar"), n(summary.unpaidAmount)),
            listOf(t(""), t("")),
            listOf(t("PEMISAHAN DOMPET"), t("PEMASUKAN"), t("PENGELUARAN"), t("PINDAH SALDO (NET)"), t("SALDO BULAN INI"), t("TOTAL AKUMULASI")),
            listOf(
                t("Dompet Tunai"),
                n(summary.cashIncome),
                n(summary.cashExpense),
                n(summary.bankToCashTransfer - summary.cashToBankTransfer),
                n(summary.netCashBalance),
                n(summary.cumulativeCashBalance),
            ),
            listOf(
                t("Transfer Bank"),
                n(summary.bankIncome),
                n(summary.bankExpense),
                n(summary.cashToBankTransfer - summary.bankToCashTransfer),
                n(summary.netBankBalance),
                n(summary.cumulativeBankBalance),
            ),
            listOf(t(""), t("")),
            listOf(t("MUTASI INTERNAL"), t("NOMINAL (IDR)")),
            listOf(t("Setor Tunai ke Bank"), n(summary.cashToBankTransfer)),
            listOf(t("Tarik Tunai dari Bank"), n(summary.bankToCashTransfer)),
        )

        val sheet2Rows = mutableListOf(
            listOf(t("No. Kuitansi"), t("Tanggal"), t("Pelanggan"), t("Paket"), t("Metode"), t("Referensi Bank"), t("Nominal (IDR)"), t("Catatan"))
        )
        payments.forEach { p ->
            sheet2Rows.add(
                listOf(
                    t(p.receiptNumber),
                    t(Formatters.formatDateFull(p.paidDateMillis)),
                    t(p.customerName),
                    t(p.packageName),
                    t(p.wallet.labelId),
                    t(p.bankReference),
                    n(p.amount),
                    t(p.notes)
                )
            )
        }

        val sheet3Rows = mutableListOf(
            listOf(t("Tanggal"), t("Judul Pengeluaran"), t("Kategori"), t("Dompet"), t("Nominal (IDR)"), t("Catatan"))
        )
        expenses.forEach { e ->
            sheet3Rows.add(
                listOf(
                    t(Formatters.formatDateFull(e.expenseDateMillis)),
                    t(e.title),
                    t(e.expenseCategory.labelId),
                    t(e.wallet.labelId),
                    n(e.amount),
                    t(e.notes)
                )
            )
        }

        val sheet4Rows = mutableListOf(
            listOf(t("Nama Pelanggan"), t("No. WA"), t("Paket"), t("Jatuh Tempo"), t("Status"), t("Tagihan (IDR)"))
        )
        billingItems.filter { it.payment == null }.forEach { item ->
            val statusLabel = when (item.status) {
                BillingStatus.OVERDUE -> "Terlambat ${-item.daysUntilDue} hari"
                BillingStatus.DUE_SOON -> if (item.daysUntilDue == 0) "Jatuh Tempo Hari Ini" else "H-${item.daysUntilDue}"
                BillingStatus.UNPAID -> "Belum Bayar"
                BillingStatus.PAID -> "Lunas"
            }
            sheet4Rows.add(
                listOf(
                    t(item.customer.name),
                    t(item.customer.phone),
                    t(item.customer.packageName),
                    t("Tgl ${item.customer.dueDay}"),
                    t(statusLabel),
                    n(item.customer.monthlyFee),
                )
            )
        }

        val sheets = mutableListOf(
            "Ringkasan" to sheet1Rows,
            "Pembayaran" to sheet2Rows,
            "Pengeluaran" to sheet3Rows,
            "Belum_Bayar" to sheet4Rows,
        )

        if (transfers.isNotEmpty()) {
            val sheet5Rows = mutableListOf(
                listOf(t("Tanggal"), t("Dari Dompet"), t("Ke Dompet"), t("Nominal (IDR)"), t("Catatan"))
            )
            transfers.forEach { trf ->
                sheet5Rows.add(
                    listOf(
                        t(Formatters.formatDateFull(trf.transferDateMillis)),
                        t(trf.source.labelId),
                        t(trf.destination.labelId),
                        n(trf.amount),
                        t(trf.notes),
                    )
                )
            }
            sheets.add("Pindah_Saldo" to sheet5Rows)
        }

        writeXlsxWorkbook(outputStream = outputStream, sheets = sheets)
    }

    private fun shareExportedFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            clipData = android.content.ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    // Minimal compliant OpenXML .xlsx generator (multi-sheet workbook, zero external deps)
    private fun writeXlsxWorkbook(outputStream: OutputStream, sheets: List<Pair<String, List<List<ExcelCell>>>>) {
        ZipOutputStream(outputStream).use { zip ->
            fun addEntry(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            val sheetOverrides = sheets.indices.joinToString("\n") { i ->
                """  <Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
            }
            addEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
$sheetOverrides
</Types>"""
            )

            addEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            )

            val sheetEntries = sheets.mapIndexed { i, (name, _) ->
                """    <sheet name="${escapeXml(name)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>"""
            }.joinToString("\n")
            addEntry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
$sheetEntries
  </sheets>
</workbook>"""
            )

            val wbRels = sheets.indices.joinToString("\n") { i ->
                """  <Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>"""
            }
            addEntry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$wbRels
</Relationships>"""
            )

            sheets.forEachIndexed { idx, (_, rows) ->
                val sheetXml = buildString {
                    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
                    rows.forEachIndexed { rIdx, cols ->
                        val rowNum = rIdx + 1
                        append("""<row r="$rowNum">""")
                        cols.forEachIndexed { cIdx, cell ->
                            val colLetter = ('A' + cIdx).toString()
                            val cellRef = "$colLetter$rowNum"
                            when (cell) {
                                is ExcelCell.Num -> append("""<c r="$cellRef"><v>${cell.value}</v></c>""")
                                is ExcelCell.Text -> append("""<c r="$cellRef" t="inlineStr"><is><t>${escapeXml(cell.value)}</t></is></c>""")
                            }
                        }
                        append("</row>")
                    }
                    append("</sheetData></worksheet>")
                }
                addEntry("xl/worksheets/sheet${idx + 1}.xml", sheetXml)
            }
        }
    }

    private fun escapeXml(input: String): String =
        input
            .filter { ch -> ch == '\t' || ch == '\n' || ch == '\r' || ch.code >= 0x20 }
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
