package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BusinessSettings
import com.example.data.Customer
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.NetKasRepository
import com.example.data.Payment
import com.example.data.WalletTransfer
import com.example.data.WalletType
import com.example.ui.theme.AmberCash
import com.example.ui.theme.BankBlue
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoMediumStyle
import com.example.ui.theme.MonoSmallStyle
import com.example.util.Formatters
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun MonthPickerDialog(
    selectedMonth: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val current = Formatters.currentYearMonth()
    // 18 previous months + current + 2 upcoming months for easy historical editing
    val months = remember(current) {
        (2 downTo -18).map { offset -> Formatters.shiftYearMonth(current, offset) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Pilih Periode Bulan", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Pilih bulan sebelumnya untuk melihat atau mengedit transaksi yang salah input.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 340.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(months) { ym ->
                    val isSelected = ym == selectedMonth
                    val isCurr = ym == current
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelect(ym)
                                onDismiss()
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = Formatters.formatYearMonthFull(ym),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isCurr) {
                                Text(
                                    text = "Bulan Ini",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else if (ym < current) {
                                Text(
                                    text = "Riwayat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun CustomerFormDialog(
    existing: Customer?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        email: String,
        address: String,
        packageName: String,
        monthlyFee: Long,
        dueDay: Long,
        active: Boolean,
        notes: String,
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone ?: "") }
    var email by remember(existing?.id) { mutableStateOf(existing?.email ?: "") }
    var address by remember(existing?.id) { mutableStateOf(existing?.address ?: "") }
    var packageName by remember(existing?.id) { mutableStateOf(existing?.packageName ?: "20 Mbps Home") }
    var feeText by remember(existing?.id) { mutableStateOf((existing?.monthlyFee ?: 150_000L).toString()) }
    var dueDayText by remember(existing?.id) { mutableStateOf((existing?.dueDay ?: 10L).toString()) }
    var active by remember(existing?.id) { mutableStateOf(existing?.active ?: true) }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes ?: "") }
    var errorMsg by remember(existing?.id) { mutableStateOf<String?>(null) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (existing == null) "Tambah Pelanggan Wi-Fi" else "Edit Pelanggan",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                if (errorMsg != null) {
                    Text(errorMsg!!, color = CrimsonExpense, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pelanggan *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input"),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. WhatsApp / HP") },
                    placeholder = { Text("08123456789") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (opsional untuk kuitansi)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat / Lokasi Router") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Paket Wi-Fi *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it.filter { ch -> ch.isDigit() }.take(9) },
                        label = { Text("Tarif / Bulan (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("customer_fee_input"),
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Jatuh Tempo (1-28)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (PPPoE / perangkat)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Status Langganan Aktif", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (existing != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonExpense),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("delete_customer_button"),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Hapus pelanggan", modifier = Modifier.size(18.dp))
                        }
                        if (showDeleteConfirm) {
                            ConfirmDeleteDialog(
                                title = "Hapus Pelanggan?",
                                message = "Data pelanggan \"${existing.name}\" akan dihapus permanen. Riwayat pembayaran sebelumnya tetap tersimpan di laporan.",
                                onConfirm = {
                                    showDeleteConfirm = false
                                    onDelete()
                                },
                                onDismiss = { showDeleteConfirm = false },
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val fee = feeText.toLongOrNull() ?: 0L
                            val due = (dueDayText.toLongOrNull() ?: 10L).coerceIn(1L, 28L)
                            when {
                                name.isBlank() -> errorMsg = "Nama pelanggan wajib diisi."
                                packageName.isBlank() -> errorMsg = "Paket Wi-Fi wajib diisi."
                                fee <= 0L -> errorMsg = "Tarif bulanan harus lebih dari Rp 0."
                                fee > 100_000_000L -> errorMsg = "Tarif bulanan maksimal Rp 100.000.000."
                                else -> onSave(name, phone, email, address, packageName, fee, due, active, notes)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_customer_button"),
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentFormDialog(
    customer: Customer,
    initialYearMonth: String,
    existingPayment: Payment?,
    onDismiss: () -> Unit,
    onSave: (
        yearMonth: String,
        amount: Long,
        wallet: WalletType,
        bankRef: String,
        receiptNumber: String,
        paidDateMillis: Long,
        notes: String,
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val stateKey = existingPayment?.id ?: "${customer.id}_$initialYearMonth"
    var yearMonth by remember(stateKey) { mutableStateOf(existingPayment?.yearMonth ?: initialYearMonth) }
    var paidDateMillis by remember(stateKey) {
        mutableStateOf(
            existingPayment?.paidDateMillis?.takeIf { it > 0L }
                ?: Formatters.defaultDateMillisForMonth(existingPayment?.yearMonth ?: initialYearMonth)
        )
    }
    var amountText by remember(stateKey) { mutableStateOf((existingPayment?.amount ?: customer.monthlyFee).toString()) }
    var wallet by remember(stateKey) { mutableStateOf(existingPayment?.wallet ?: WalletType.CASH) }
    var bankReference by remember(stateKey) { mutableStateOf(existingPayment?.bankReference ?: "") }
    var receiptNumber by remember(stateKey) {
        mutableStateOf(
            existingPayment?.receiptNumber?.ifBlank { null }
                ?: NetKasRepository.generateReceiptNumber(yearMonth, customer.id)
        )
    }
    var notes by remember(stateKey) { mutableStateOf(existingPayment?.notes ?: "") }
    var errorMsg by remember(stateKey) { mutableStateOf<String?>(null) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existingPayment == null) "Catat Pembayaran Wi-Fi" else "Edit Pembayaran",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "${customer.name} • ${customer.packageName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                // Period adjustment for correcting previous month transactions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = {
                            val next = Formatters.shiftYearMonth(yearMonth, -1)
                            yearMonth = next
                            paidDateMillis = Formatters.defaultDateMillisForMonth(next)
                            if (existingPayment == null) {
                                receiptNumber = NetKasRepository.generateReceiptNumber(next, customer.id)
                            }
                        }) {
                            Text("← Bulan Lalu")
                        }
                        Text(
                            text = Formatters.formatYearMonthFull(yearMonth),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = {
                            val next = Formatters.shiftYearMonth(yearMonth, 1)
                            yearMonth = next
                            paidDateMillis = Formatters.defaultDateMillisForMonth(next)
                            if (existingPayment == null) {
                                receiptNumber = NetKasRepository.generateReceiptNumber(next, customer.id)
                            }
                        }) {
                            Text("Maju →")
                        }
                    }
                }

                TransactionDateRow(
                    label = "Tanggal Pembayaran",
                    dateMillis = paidDateMillis,
                    onDateSelected = { picked -> paidDateMillis = picked },
                    testTag = "payment_date_picker",
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = CrimsonExpense, style = MaterialTheme.typography.bodySmall)
                }

                // Wallet separation selector (Dompet Tunai vs Transfer Bank)
                Text(
                    text = "Masuk ke Dompet Pembukuan *",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WalletSelectCard(
                        title = "Dompet Tunai",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        selected = wallet == WalletType.CASH,
                        accent = AmberCash,
                        onClick = { wallet = WalletType.CASH },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_wallet_cash"),
                    )
                    WalletSelectCard(
                        title = "Transfer Bank",
                        icon = Icons.Rounded.AccountBalance,
                        selected = wallet == WalletType.BANK_TRANSFER,
                        accent = BankBlue,
                        onClick = { wallet = WalletType.BANK_TRANSFER },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_wallet_bank"),
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }.take(9) },
                    label = { Text("Nominal Dibayar (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                )

                if (wallet == WalletType.BANK_TRANSFER) {
                    OutlinedTextField(
                        value = bankReference,
                        onValueChange = { bankReference = it },
                        label = { Text("Bank / No. Referensi Transfer") },
                        placeholder = { Text("Contoh: BCA / BRI #8821") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = receiptNumber,
                    onValueChange = { receiptNumber = it },
                    label = { Text("Nomor Kuitansi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Koreksi Input (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (existingPayment != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonExpense),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("delete_payment_button"),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Batalkan pembayaran", modifier = Modifier.size(18.dp))
                        }
                        if (showDeleteConfirm) {
                            ConfirmDeleteDialog(
                                title = "Batalkan Pembayaran?",
                                message = "Catatan pembayaran ${customer.name} untuk periode ${Formatters.formatYearMonthFull(existingPayment.yearMonth)} akan dihapus dan statusnya kembali menjadi belum bayar.",
                                onConfirm = {
                                    showDeleteConfirm = false
                                    onDelete()
                                },
                                onDismiss = { showDeleteConfirm = false },
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val amount = amountText.toLongOrNull() ?: 0L
                            when {
                                amount <= 0L -> errorMsg = "Nominal harus lebih dari Rp 0."
                                amount > 100_000_000L -> errorMsg = "Nominal pembayaran maksimal Rp 100.000.000."
                                else -> {
                                    onSave(
                                        yearMonth,
                                        amount,
                                        wallet,
                                        if (wallet == WalletType.BANK_TRANSFER) bankReference else "",
                                        receiptNumber,
                                        paidDateMillis,
                                        notes,
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_payment_button"),
                    ) {
                        Text("Simpan Lunas")
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseFormDialog(
    initialYearMonth: String,
    existing: Expense?,
    onDismiss: () -> Unit,
    onSave: (
        yearMonth: String,
        title: String,
        category: ExpenseCategory,
        amount: Long,
        wallet: WalletType,
        expenseDateMillis: Long,
        notes: String,
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val stateKey = existing?.id ?: initialYearMonth
    var yearMonth by remember(stateKey) { mutableStateOf(existing?.yearMonth ?: initialYearMonth) }
    var expenseDateMillis by remember(stateKey) {
        mutableStateOf(
            existing?.expenseDateMillis?.takeIf { it > 0L }
                ?: Formatters.defaultDateMillisForMonth(existing?.yearMonth ?: initialYearMonth)
        )
    }
    var title by remember(stateKey) { mutableStateOf(existing?.title ?: "") }
    var category by remember(stateKey) { mutableStateOf(existing?.expenseCategory ?: ExpenseCategory.BANDWIDTH) }
    var amountText by remember(stateKey) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var wallet by remember(stateKey) { mutableStateOf(existing?.wallet ?: WalletType.CASH) }
    var notes by remember(stateKey) { mutableStateOf(existing?.notes ?: "") }
    var errorMsg by remember(stateKey) { mutableStateOf<String?>(null) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (existing == null) "Catat Pengeluaran" else "Edit Pengeluaran",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                // Period adjustment for previous-month corrections
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = {
                            val next = Formatters.shiftYearMonth(yearMonth, -1)
                            yearMonth = next
                            expenseDateMillis = Formatters.defaultDateMillisForMonth(next)
                        }) {
                            Text("← Bulan Lalu")
                        }
                        Text(
                            text = Formatters.formatYearMonthFull(yearMonth),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = {
                            val next = Formatters.shiftYearMonth(yearMonth, 1)
                            yearMonth = next
                            expenseDateMillis = Formatters.defaultDateMillisForMonth(next)
                        }) {
                            Text("Maju →")
                        }
                    }
                }

                TransactionDateRow(
                    label = "Tanggal Pengeluaran",
                    dateMillis = expenseDateMillis,
                    onDateSelected = { picked ->
                        expenseDateMillis = picked
                        yearMonth = yearMonthFromLocalMillis(picked)
                    },
                    testTag = "expense_date_picker",
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = CrimsonExpense, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Pengeluaran *") },
                    placeholder = { Text("Contoh: Bayar Bandwidth ISP Induk") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input"),
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }.take(9) },
                    label = { Text("Nominal (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                )

                Text("Kategori Pengeluaran", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpenseCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelId) },
                        )
                    }
                }

                Text("Sumber Dana Dompet", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WalletSelectCard(
                        title = "Dompet Tunai",
                        icon = Icons.Rounded.AccountBalanceWallet,
                        selected = wallet == WalletType.CASH,
                        accent = AmberCash,
                        onClick = { wallet = WalletType.CASH },
                        modifier = Modifier.weight(1f),
                    )
                    WalletSelectCard(
                        title = "Transfer Bank",
                        icon = Icons.Rounded.AccountBalance,
                        selected = wallet == WalletType.BANK_TRANSFER,
                        accent = BankBlue,
                        onClick = { wallet = WalletType.BANK_TRANSFER },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (existing != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonExpense),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("delete_expense_button"),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Hapus pengeluaran", modifier = Modifier.size(18.dp))
                        }
                        if (showDeleteConfirm) {
                            ConfirmDeleteDialog(
                                title = "Hapus Pengeluaran?",
                                message = "Catatan pengeluaran \"${existing.title}\" sebesar ${Formatters.formatRupiah(existing.amount)} akan dihapus dari pembukuan.",
                                onConfirm = {
                                    showDeleteConfirm = false
                                    onDelete()
                                },
                                onDismiss = { showDeleteConfirm = false },
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val amount = amountText.toLongOrNull() ?: 0L
                            when {
                                title.isBlank() -> errorMsg = "Judul pengeluaran wajib diisi."
                                amount <= 0L -> errorMsg = "Nominal pengeluaran harus lebih dari Rp 0."
                                amount > 100_000_000L -> errorMsg = "Nominal pengeluaran maksimal Rp 100.000.000."
                                else -> {
                                    onSave(yearMonth, title, category, amount, wallet, expenseDateMillis, notes)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_expense_button"),
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun WalletTransferDialog(
    initialYearMonth: String,
    existing: WalletTransfer?,
    onDismiss: () -> Unit,
    onSave: (
        yearMonth: String,
        fromWallet: WalletType,
        toWallet: WalletType,
        amount: Long,
        transferDateMillis: Long,
        notes: String,
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val stateKey = existing?.id ?: initialYearMonth
    var yearMonth by remember(stateKey) { mutableStateOf(existing?.yearMonth ?: initialYearMonth) }
    var transferDateMillis by remember(stateKey) {
        mutableStateOf(
            existing?.transferDateMillis?.takeIf { it > 0L }
                ?: Formatters.defaultDateMillisForMonth(existing?.yearMonth ?: initialYearMonth)
        )
    }
    var fromWallet by remember(stateKey) { mutableStateOf(existing?.source ?: WalletType.CASH) }
    val toWallet = if (fromWallet == WalletType.CASH) WalletType.BANK_TRANSFER else WalletType.CASH
    var amountText by remember(stateKey) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var notes by remember(stateKey) { mutableStateOf(existing?.notes ?: "") }
    var errorMsg by remember(stateKey) { mutableStateOf<String?>(null) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (existing == null) "Pindah Saldo Dompet" else "Edit Pindah Saldo",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Setor uang tunai ke rekening bank atau tarik tunai (tidak mengubah laba bersih).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                // Period selector
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = {
                            val next = Formatters.shiftYearMonth(yearMonth, -1)
                            yearMonth = next
                            transferDateMillis = Formatters.defaultDateMillisForMonth(next)
                        }) {
                            Text("← Bulan Lalu")
                        }
                        Text(
                            text = Formatters.formatYearMonthFull(yearMonth),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = {
                            val next = Formatters.shiftYearMonth(yearMonth, 1)
                            yearMonth = next
                            transferDateMillis = Formatters.defaultDateMillisForMonth(next)
                        }) {
                            Text("Maju →")
                        }
                    }
                }

                TransactionDateRow(
                    label = "Tanggal Pindah Saldo",
                    dateMillis = transferDateMillis,
                    onDateSelected = { picked ->
                        transferDateMillis = picked
                        yearMonth = yearMonthFromLocalMillis(picked)
                    },
                    testTag = "transfer_date_picker",
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = CrimsonExpense, style = MaterialTheme.typography.bodySmall)
                }

                Text("Arah Perpindahan Saldo *", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WalletSelectCard(
                        title = "Tunai → Bank",
                        icon = Icons.Rounded.SwapHoriz,
                        selected = fromWallet == WalletType.CASH,
                        accent = BankBlue,
                        onClick = { fromWallet = WalletType.CASH },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transfer_cash_to_bank"),
                    )
                    WalletSelectCard(
                        title = "Bank → Tunai",
                        icon = Icons.Rounded.SwapHoriz,
                        selected = fromWallet == WalletType.BANK_TRANSFER,
                        accent = AmberCash,
                        onClick = { fromWallet = WalletType.BANK_TRANSFER },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transfer_bank_to_cash"),
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() }.take(9) },
                    label = { Text("Nominal Dipindahkan (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_amount_input"),
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan (contoh: Setor tunai via ATM BCA)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    if (existing != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonExpense),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("delete_transfer_button"),
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Hapus pindah saldo", modifier = Modifier.size(18.dp))
                        }
                        if (showDeleteConfirm) {
                            ConfirmDeleteDialog(
                                title = "Hapus Pindah Saldo?",
                                message = "Mutasi pindah saldo sebesar ${Formatters.formatRupiah(existing.amount)} akan dihapus dan saldo dompet disesuaikan kembali.",
                                onConfirm = {
                                    showDeleteConfirm = false
                                    onDelete()
                                },
                                onDismiss = { showDeleteConfirm = false },
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val amount = amountText.toLongOrNull() ?: 0L
                            when {
                                amount <= 0L -> errorMsg = "Nominal pindah saldo harus lebih dari Rp 0."
                                amount > 100_000_000L -> errorMsg = "Nominal pindah saldo maksimal Rp 100.000.000."
                                else -> onSave(yearMonth, fromWallet, toWallet, amount, transferDateMillis, notes)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_transfer_button"),
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptPreviewDialog(
    payment: Payment,
    customer: Customer?,
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onPrint: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareEmail: () -> Unit,
    onSaveImageToDevice: () -> Unit,
) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Kuitansi Pembayaran", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup kuitansi")
                    }
                }

                // Crisp Visual Receipt Card Preview
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFD5DFDA)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EmeraldPrimary)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = settings.businessName.ifBlank { "NetKas Wi-Fi" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (settings.businessPhone.isNotBlank()) {
                                    Text(
                                        text = "WA: ${settings.businessPhone}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFD5F2E7),
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "PEMBAYARAN LUNAS",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            ReceiptLine("No. Kuitansi", payment.receiptNumber)
                            ReceiptLine("Periode", Formatters.formatYearMonthFull(payment.yearMonth))
                            ReceiptLine("Tanggal", Formatters.formatDateFull(payment.paidDateMillis))
                            ReceiptLine("Pelanggan", payment.customerName)
                            ReceiptLine("Paket", payment.packageName)
                            ReceiptLine("Metode", payment.wallet.labelId)
                            if (payment.bankReference.isNotBlank()) {
                                ReceiptLine("Referensi", payment.bankReference)
                            }

                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F8F5),
                                border = BorderStroke(1.dp, EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "TOTAL DITERIMA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                    )
                                    Text(
                                        text = Formatters.formatRupiah(payment.amount),
                                        style = MonoMediumStyle,
                                        color = EmeraldPrimary,
                                    )
                                }
                            }

                            if (!customer?.phone.isNullOrBlank()) {
                                Text(
                                    text = "WA tujuan: ${customer!!.phone}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (!customer?.email.isNullOrBlank()) {
                                Text(
                                    text = "Email tujuan: ${customer!!.email}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }

                // Print & Share Photo Actions
                Button(
                    onClick = onPrint,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("print_receipt_button"),
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cetak Kuitansi (Printer)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onShareWhatsApp,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_whatsapp_button"),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Foto via WA", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onShareEmail,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_email_button"),
                    ) {
                        Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Foto via Email", style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedButton(
                    onClick = onSaveImageToDevice,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_receipt_image_button"),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan Foto Kuitansi (PNG) ke HP")
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpense),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_delete_button"),
            ) {
                Text("Ya, Hapus")
            }
        },
    )
}

@Composable
fun BusinessSettingsDialog(
    settings: BusinessSettings,
    onDismiss: () -> Unit,
    onSave: (
        businessName: String,
        businessPhone: String,
        businessAddress: String,
        bankAccountInfo: String,
        monthlyExpenseBudget: Long,
        reminderDaysBefore: Long,
        remindersEnabled: Boolean,
    ) -> Unit,
) {
    var businessName by remember { mutableStateOf(settings.businessName) }
    var businessPhone by remember { mutableStateOf(settings.businessPhone) }
    var businessAddress by remember { mutableStateOf(settings.businessAddress) }
    var bankAccountInfo by remember { mutableStateOf(settings.bankAccountInfo) }
    var budgetText by remember { mutableStateOf(settings.monthlyExpenseBudget.toString()) }
    var reminderDaysText by remember { mutableStateOf(settings.reminderDaysBefore.toString()) }
    var remindersEnabled by remember { mutableStateOf(settings.remindersEnabled) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Pengaturan Usaha & Pengingat", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Tutup")
                    }
                }

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Nama Usaha Wi-Fi (tampil di kuitansi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = businessPhone,
                    onValueChange = { businessPhone = it },
                    label = { Text("No. WhatsApp / CS Usaha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = businessAddress,
                    onValueChange = { businessAddress = it },
                    label = { Text("Alamat Usaha") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = bankAccountInfo,
                    onValueChange = { bankAccountInfo = it },
                    label = { Text("Info Rekening Transfer Bank") },
                    placeholder = { Text("Contoh: BCA 1234567890 a.n. NetKas") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { ch -> ch.isDigit() }.take(11) },
                    label = { Text("Target Anggaran Pengeluaran Bulanan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reminderDaysText,
                    onValueChange = { reminderDaysText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Ingatkan H- berapa hari sebelum jatuh tempo (0-15)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notifikasi Pengingat Jatuh Tempo", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Peringatkan otomatis saat ada tagihan mendekati jatuh tempo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = remindersEnabled, onCheckedChange = { remindersEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            onSave(
                                businessName,
                                businessPhone,
                                businessAddress,
                                bankAccountInfo,
                                budgetText.toLongOrNull() ?: 0L,
                                (reminderDaysText.toLongOrNull() ?: 3L).coerceIn(0L, 15L),
                                remindersEnabled,
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("save_settings_button"),
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletSelectCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B),
        )
        Text(
            text = value,
            style = MonoSmallStyle,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDateRow(
    label: String,
    dateMillis: Long,
    onDateSelected: (Long) -> Unit,
    testTag: String,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDatePicker = true }
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Formatters.formatDateFull(dateMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Text(
                text = "Ubah",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localMillisToUtcDatePickerMillis(dateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            onDateSelected(utcDatePickerMillisToLocalNoon(utcMillis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun localMillisToUtcDatePickerMillis(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0,
        )
    }
    return utcCal.timeInMillis
}

private fun utcDatePickerMillisToLocalNoon(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val localCal = Calendar.getInstance().apply {
        clear()
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
}

private fun yearMonthFromLocalMillis(localMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = localMillis }
    return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}
