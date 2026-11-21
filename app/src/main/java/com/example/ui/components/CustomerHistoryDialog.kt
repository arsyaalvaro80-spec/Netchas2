package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Customer
import com.example.data.Payment
import com.example.data.WalletType
import com.example.ui.theme.AmberCash
import com.example.ui.theme.BankBlue
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoSmallStyle
import com.example.util.Formatters

@Composable
fun CustomerHistoryDialog(
    customer: Customer,
    selectedMonth: String,
    allPayments: List<Payment>,
    onDismiss: () -> Unit,
    onEditCustomer: () -> Unit,
    onRecordPaymentForSelectedMonth: () -> Unit,
    onEditPayment: (Payment) -> Unit,
    onOpenReceipt: (Payment) -> Unit,
    onSendWhatsAppReminder: () -> Unit,
) {
    val customerPayments = remember(allPayments, customer.id) {
        allPayments
            .filter { it.customerId == customer.id }
            .sortedWith(compareByDescending<Payment> { it.yearMonth }.thenByDescending { it.paidDateMillis })
    }
    val totalPaidAmount = remember(customerPayments) { customerPayments.sumOf { it.amount } }
    val paidSelectedMonth = remember(customerPayments, selectedMonth) {
        customerPayments.any { it.yearMonth == selectedMonth }
    }
    val cashCount = remember(customerPayments) { customerPayments.count { it.wallet == WalletType.CASH } }
    val bankCount = remember(customerPayments) { customerPayments.count { it.wallet == WalletType.BANK_TRANSFER } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (customer.active) EmeraldPrimary.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    text = if (customer.active) "Aktif" else "Nonaktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (customer.active) EmeraldPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${customer.packageName} • ${Formatters.formatRupiah(customer.monthlyFee)}/bln • Tgl ${customer.dueDay}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (customer.phone.isNotBlank() || customer.address.isNotBlank()) {
                            val details = listOfNotNull(
                                customer.phone.takeIf { it.isNotBlank() }?.let { "WA: $it" },
                                customer.address.takeIf { it.isNotBlank() },
                            ).joinToString(" • ")
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup riwayat pelanggan")
                    }
                }

                // Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HistoryMiniStat(
                        label = "Total Bayar",
                        value = Formatters.formatRupiahCompact(totalPaidAmount),
                        accent = EmeraldPrimary,
                        modifier = Modifier.weight(1.2f),
                    )
                    HistoryMiniStat(
                        label = "Transaksi",
                        value = "${customerPayments.size} bln",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(0.85f),
                    )
                    HistoryMiniStat(
                        label = "Tunai / Bank",
                        value = "$cashCount / $bankCount",
                        accent = BankBlue,
                        modifier = Modifier.weight(0.95f),
                    )
                }

                // Quick Customer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onEditCustomer,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_edit_customer_button"),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Edit Profil", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onSendWhatsAppReminder,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("history_whatsapp_button"),
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Ingatkan WA", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (customer.active && !paidSelectedMonth) {
                    Button(
                        onClick = onRecordPaymentForSelectedMonth,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_record_payment_button"),
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Catat Bayar ${Formatters.formatYearMonthShort(selectedMonth)}")
                    }
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Riwayat Pembayaran (${customerPayments.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (customerPayments.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Belum ada riwayat pembayaran tercatat untuk pelanggan ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(customerPayments, key = { it.id }) { payment ->
                            val walletColor = if (payment.wallet == WalletType.CASH) AmberCash else BankBlue
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = Formatters.formatYearMonthFull(payment.yearMonth),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "${Formatters.formatDateFull(payment.paidDateMillis)} • ${payment.wallet.labelId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = walletColor,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = Formatters.formatRupiah(payment.amount),
                                            style = MonoSmallStyle,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(onClick = { onEditPayment(payment) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Edit pembayaran ${payment.yearMonth}",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                        IconButton(onClick = { onOpenReceipt(payment) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Receipt,
                                                contentDescription = "Lihat kuitansi ${payment.yearMonth}",
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMiniStat(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.09f),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MonoSmallStyle,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
