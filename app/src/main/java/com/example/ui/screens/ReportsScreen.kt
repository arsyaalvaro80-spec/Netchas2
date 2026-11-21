package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.ExpenseCategoryDonutChart
import com.example.ui.components.MonthPeriodBar
import com.example.ui.components.SixMonthPerformanceChart
import com.example.ui.components.WalletSplitBar
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoMediumStyle
import com.example.ui.theme.MonoSmallStyle
import com.example.ui.theme.SlateSecondary
import com.example.util.Formatters
import com.example.viewmodel.NetKasDashboardData

@Composable
fun ReportsScreen(
    data: NetKasDashboardData,
    userEmail: String?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onExportPdf: () -> Unit,
    onSavePdfToDevice: () -> Unit,
    onExportExcel: () -> Unit,
    onSaveExcelToDevice: () -> Unit,
    onOpenSettings: () -> Unit,
    onSendDueNotification: () -> Unit,
    onSignOut: () -> Unit,
) {
    val summary = data.summary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Laporan & Ekspor Keuangan",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Analisis performa 6 bulan, proporsi dompet, dan ekspor otomatis PDF / Excel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            MonthPeriodBar(
                selectedMonth = data.selectedMonth,
                isHistoricalMonth = data.isHistoricalMonth,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                onOpenPicker = onOpenMonthPicker,
                onJumpCurrent = onJumpCurrentMonth,
            )
        }

        // Automatic PDF & Excel Export Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ekspor Laporan Bulanan Otomatis",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Buat laporan lengkap periode ${Formatters.formatYearMonthFull(data.selectedMonth)} (Laba Bersih, Dompet Tunai vs Transfer Bank, Pembayaran, Pengeluaran & Belum Bayar).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))

                    // PDF Export Row (Share & Save to Device)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onExportPdf,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("export_pdf_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bagikan PDF",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        OutlinedButton(
                            onClick = onSavePdfToDevice,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonExpense),
                            border = BorderStroke(1.dp, CrimsonExpense.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_pdf_device_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Simpan PDF",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Excel Export Row (Share & Save to Device)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onExportExcel,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("export_excel_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Bagikan Excel",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        OutlinedButton(
                            onClick = onSaveExcelToDevice,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_excel_device_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Simpan Excel",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }

        // 6-Month Financial Performance Chart
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Performa Keuangan 6 Bulan Terakhir",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Ketuk label bulan di bawah grafik untuk melihat detail angka",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    SixMonthPerformanceChart(points = data.trendSixMonths)
                }
            }
        }

        // Profit & Loss + Cash vs Bank Breakdown Summary
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ringkasan Laba Rugi & Dompet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    ReportStatRow("Total Pemasukan", Formatters.formatRupiah(summary.totalIncome), EmeraldPrimary)
                    ReportStatRow("Total Pengeluaran", Formatters.formatRupiah(summary.totalExpense), CrimsonExpense)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Keuntungan Bersih",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = Formatters.formatRupiah(summary.netProfit),
                            style = MonoMediumStyle,
                            color = if (summary.netProfit >= 0) EmeraldPrimary else CrimsonExpense,
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Proporsi Pemasukan Pelanggan",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    WalletSplitBar(
                        cashAmount = summary.cashIncome,
                        bankAmount = summary.bankIncome,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (summary.cashToBankTransfer > 0L) {
                        ReportStatRow("Setor Tunai → Bank", Formatters.formatRupiah(summary.cashToBankTransfer))
                    }
                    if (summary.bankToCashTransfer > 0L) {
                        ReportStatRow("Tarik Bank → Tunai", Formatters.formatRupiah(summary.bankToCashTransfer))
                    }
                    ReportStatRow("Saldo Bersih Dompet Tunai (Bulan Ini)", Formatters.formatRupiah(summary.netCashBalance))
                    ReportStatRow("Saldo Bersih Transfer Bank (Bulan Ini)", Formatters.formatRupiah(summary.netBankBalance))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    ReportStatRow(
                        "Total Akumulasi Dompet Tunai",
                        Formatters.formatRupiah(summary.cumulativeCashBalance),
                        if (summary.cumulativeCashBalance >= 0) EmeraldPrimary else CrimsonExpense,
                    )
                    ReportStatRow(
                        "Total Akumulasi Transfer Bank",
                        Formatters.formatRupiah(summary.cumulativeBankBalance),
                        if (summary.cumulativeBankBalance >= 0) EmeraldPrimary else CrimsonExpense,
                    )
                }
            }
        }

        // Expense Breakdown Chart in Reports
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Komposisi Pengeluaran Bulanan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    ExpenseCategoryDonutChart(
                        slices = data.expenseByCategory,
                        totalExpense = summary.totalExpense,
                    )
                }
            }
        }

        // Business Settings, Reminders & Account Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Profil Usaha, Notifikasi & Akun",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = "Akun Cloud Aktif: $userEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reports_settings_button"),
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Profil & Budget")
                        }
                        OutlinedButton(
                            onClick = onSendDueNotification,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cek Pengingat")
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonExpense),
                        border = BorderStroke(1.dp, CrimsonExpense.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_sign_out))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun ReportStatRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = SlateSecondary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MonoSmallStyle,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}
