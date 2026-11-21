package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BillingStatus
import com.example.data.Customer
import com.example.data.CustomerBillingItem
import com.example.data.Payment
import com.example.data.WalletType
import com.example.ui.components.MonthPeriodBar
import com.example.ui.theme.AmberCash
import com.example.ui.theme.BankBlue
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoSmallStyle
import com.example.util.Formatters
import com.example.viewmodel.NetKasDashboardData

private enum class BillingFilter(val label: String) {
    ALL("Semua"),
    UNPAID("Belum Bayar"),
    URGENT("Jatuh Tempo"),
    PAID_CASH("Bayar Tunai"),
    PAID_BANK("Transfer Bank"),
    PAID("Semua Lunas"),
    INACTIVE("Nonaktif"),
}

@Composable
fun BillingScreen(
    data: NetKasDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onAddCustomer: () -> Unit,
    onEditCustomer: (Customer) -> Unit,
    onOpenCustomerHistory: (Customer) -> Unit,
    onReactivateCustomer: (Customer) -> Unit,
    onRecordPayment: (Customer) -> Unit,
    onEditPayment: (Customer, Payment) -> Unit,
    onOpenReceipt: (Payment) -> Unit,
    onSendWhatsAppReminder: (CustomerBillingItem) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(BillingFilter.ALL) }

    val cashPaidCount = remember(data.billingItems) {
        data.billingItems.count { it.payment?.wallet == WalletType.CASH }
    }
    val bankPaidCount = remember(data.billingItems) {
        data.billingItems.count { it.payment?.wallet == WalletType.BANK_TRANSFER }
    }

    val filteredInactive = remember(data.inactiveCustomers, searchQuery) {
        data.inactiveCustomers.filter { customer ->
            searchQuery.isBlank() ||
                customer.name.contains(searchQuery, ignoreCase = true) ||
                customer.packageName.contains(searchQuery, ignoreCase = true) ||
                customer.address.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredItems = remember(data.billingItems, searchQuery, filter) {
        if (filter == BillingFilter.INACTIVE) emptyList()
        else data.billingItems.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.customer.name.contains(searchQuery, ignoreCase = true) ||
                item.customer.packageName.contains(searchQuery, ignoreCase = true) ||
                item.customer.address.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filter) {
                BillingFilter.ALL -> true
                BillingFilter.UNPAID -> item.payment == null
                BillingFilter.URGENT -> item.status == BillingStatus.OVERDUE || item.status == BillingStatus.DUE_SOON
                BillingFilter.PAID_CASH -> item.payment?.wallet == WalletType.CASH
                BillingFilter.PAID_BANK -> item.payment?.wallet == WalletType.BANK_TRANSFER
                BillingFilter.PAID -> item.payment != null
                BillingFilter.INACTIVE -> false
            }
            matchesQuery && matchesFilter
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "Tagihan & Pelanggan Wi-Fi",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Lacak pembayaran bulanan, pisahkan tunai & transfer, atau koreksi bulan lalu",
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

            // Summary KPI Strip: Tunai vs Transfer Bank vs Belum Bayar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BillingMiniMetric(
                            label = "Tunai ($cashPaidCount org)",
                            value = Formatters.formatRupiahCompact(data.summary.cashIncome),
                            color = AmberCash,
                            isSelected = filter == BillingFilter.PAID_CASH,
                            onClick = {
                                filter = if (filter == BillingFilter.PAID_CASH) BillingFilter.ALL else BillingFilter.PAID_CASH
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("billing_cash_summary_card"),
                        )
                        BillingMiniMetric(
                            label = "Transfer ($bankPaidCount org)",
                            value = Formatters.formatRupiahCompact(data.summary.bankIncome),
                            color = BankBlue,
                            isSelected = filter == BillingFilter.PAID_BANK,
                            onClick = {
                                filter = if (filter == BillingFilter.PAID_BANK) BillingFilter.ALL else BillingFilter.PAID_BANK
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("billing_bank_summary_card"),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BillingMiniMetric(
                            label = "Total Lunas (${data.summary.paidCount})",
                            value = Formatters.formatRupiahCompact(data.summary.totalIncome),
                            color = EmeraldPrimary,
                            isSelected = filter == BillingFilter.PAID,
                            onClick = {
                                filter = if (filter == BillingFilter.PAID) BillingFilter.ALL else BillingFilter.PAID
                            },
                            modifier = Modifier.weight(1f),
                        )
                        BillingMiniMetric(
                            label = "Belum Bayar (${data.summary.unpaidCount})",
                            value = Formatters.formatRupiahCompact(data.summary.unpaidAmount),
                            color = CrimsonExpense,
                            isSelected = filter == BillingFilter.UNPAID,
                            onClick = {
                                filter = if (filter == BillingFilter.UNPAID) BillingFilter.ALL else BillingFilter.UNPAID
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nama pelanggan, paket, atau alamat...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("billing_search_input"),
                )
            }

            // Status Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BillingFilter.entries) { f ->
                        val chipLabel = if (f == BillingFilter.INACTIVE && data.inactiveCustomers.isNotEmpty()) {
                            "${f.label} (${data.inactiveCustomers.size})"
                        } else {
                            f.label
                        }
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(chipLabel) },
                            modifier = Modifier.testTag("billing_filter_${f.name.lowercase()}"),
                        )
                    }
                }
            }

            if (filter == BillingFilter.INACTIVE) {
                if (filteredInactive.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Wifi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(40.dp),
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Tidak ada pelanggan nonaktif",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Pelanggan yang dinonaktifkan sementara akan tampil di sini agar mudah diaktifkan kembali.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(filteredInactive, key = { "inactive_${it.id}" }) { customer ->
                        InactiveCustomerCard(
                            customer = customer,
                            onOpenHistory = { onOpenCustomerHistory(customer) },
                            onEditCustomer = { onEditCustomer(customer) },
                            onReactivate = { onReactivateCustomer(customer) },
                        )
                    }
                }
            } else if (filteredItems.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (data.customers.isEmpty()) "Belum ada pelanggan Wi-Fi terdaftar"
                                else "Tidak ada pelanggan sesuai filter ini",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Tekan tombol Tambah Pelanggan untuk mulai melacak tagihan bulanan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.customer.id }) { item ->
                    CustomerBillingCard(
                        item = item,
                        onOpenHistory = { onOpenCustomerHistory(item.customer) },
                        onEditCustomer = { onEditCustomer(item.customer) },
                        onPayNow = { onRecordPayment(item.customer) },
                        onEditPayment = { payment -> onEditPayment(item.customer, payment) },
                        onOpenReceipt = { payment -> onOpenReceipt(payment) },
                        onSendReminder = { onSendWhatsAppReminder(item) },
                    )
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }

        ExtendedFloatingActionButton(
            onClick = onAddCustomer,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.action_add_customer)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_customer_fab"),
        )
    }
}

@Composable
private fun BillingMiniMetric(
    label: String,
    value: String,
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MonoSmallStyle,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun CustomerBillingCard(
    item: CustomerBillingItem,
    onOpenHistory: () -> Unit,
    onEditCustomer: () -> Unit,
    onPayNow: () -> Unit,
    onEditPayment: (Payment) -> Unit,
    onOpenReceipt: (Payment) -> Unit,
    onSendReminder: () -> Unit,
) {
    val (statusText, statusBg, statusFg) = when (item.status) {
        BillingStatus.PAID -> Triple("Lunas", EmeraldPrimary.copy(alpha = 0.14f), EmeraldPrimary)
        BillingStatus.OVERDUE -> Triple("Jatuh Tempo", CrimsonExpense.copy(alpha = 0.14f), CrimsonExpense)
        BillingStatus.DUE_SOON -> Triple(
            if (item.daysUntilDue == 0) "Jatuh Tempo Hari Ini" else "H-${item.daysUntilDue}",
            AmberCash.copy(alpha = 0.16f),
            AmberCash
        )
        BillingStatus.UNPAID -> Triple("Belum Bayar", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenHistory)
            .testTag("customer_billing_card_${item.customer.id}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBg,
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusFg,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${item.customer.packageName} • Jatuh tempo tgl ${item.customer.dueDay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.customer.address.isNotBlank()) {
                        Text(
                            text = item.customer.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(onClick = onEditCustomer) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit data pelanggan",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val payment = item.payment
            if (payment != null) {
                val walletColor = if (payment.wallet == WalletType.CASH) AmberCash else BankBlue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = Formatters.formatRupiah(payment.amount),
                            style = MonoSmallStyle,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                        )
                        Text(
                            text = "${payment.wallet.labelId} • ${Formatters.formatDateFull(payment.paidDateMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = walletColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onEditPayment(payment) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("Edit", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { onOpenReceipt(payment) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_receipt_button_${item.customer.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Kuitansi", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = Formatters.formatRupiah(item.customer.monthlyFee),
                        style = MonoSmallStyle,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSendReminder,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("Ingatkan WA", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onPayNow,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("pay_customer_button_${item.customer.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Bayar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InactiveCustomerCard(
    customer: Customer,
    onOpenHistory: () -> Unit,
    onEditCustomer: () -> Unit,
    onReactivate: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenHistory)
            .testTag("inactive_customer_card_${customer.id}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = "Nonaktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${customer.packageName} • ${Formatters.formatRupiah(customer.monthlyFee)}/bln",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(onClick = onEditCustomer) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit data pelanggan nonaktif",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onReactivate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("reactivate_customer_button_${customer.id}"),
                ) {
                    Text("Aktifkan Kembali", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
