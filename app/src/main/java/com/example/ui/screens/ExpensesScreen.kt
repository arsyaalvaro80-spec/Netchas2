package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.Expense
import com.example.data.ExpenseCategory
import com.example.data.WalletType
import com.example.ui.components.ExpenseCategoryDonutChart
import com.example.ui.components.MonthPeriodBar
import com.example.ui.components.categoryColor
import com.example.ui.theme.AmberCash
import com.example.ui.theme.BankBlue
import com.example.ui.theme.CrimsonExpense
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MonoMediumStyle
import com.example.ui.theme.MonoSmallStyle
import com.example.util.Formatters
import com.example.viewmodel.NetKasDashboardData

@Composable
fun ExpensesScreen(
    data: NetKasDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenMonthPicker: () -> Unit,
    onJumpCurrentMonth: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onOpenBudgetSettings: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var walletFilter by remember { mutableStateOf<WalletType?>(null) }
    var categoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }

    val filteredExpenses = remember(data.monthExpenses, searchQuery, walletFilter, categoryFilter) {
        data.monthExpenses.filter { exp ->
            val matchesWallet = walletFilter == null || exp.wallet == walletFilter
            val matchesCategory = categoryFilter == null || exp.expenseCategory == categoryFilter
            val matchesQuery = searchQuery.isBlank() ||
                exp.title.contains(searchQuery, ignoreCase = true) ||
                exp.notes.contains(searchQuery, ignoreCase = true) ||
                exp.expenseCategory.labelId.contains(searchQuery, ignoreCase = true)
            matchesWallet && matchesCategory && matchesQuery
        }
    }

    val budget = data.settings.monthlyExpenseBudget
    val totalExpense = data.summary.totalExpense
    val budgetRatio = if (budget > 0L) (totalExpense.toFloat() / budget.toFloat()) else 0f
    val budgetColor = when {
        budgetRatio >= 1f -> CrimsonExpense
        budgetRatio >= 0.8f -> AmberCash
        else -> EmeraldPrimary
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "Pengeluaran & Anggaran",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Monitor anggaran bulanan dan sumber dana kas tunai / transfer bank",
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

            // Monthly Budget Monitoring Card
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Monitoring Anggaran Bulanan",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (budget > 0L) {
                                        "Target: ${Formatters.formatRupiah(budget)}"
                                    } else {
                                        "Belum ada target anggaran"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onOpenBudgetSettings)
                                    .testTag("edit_budget_button"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = "Atur anggaran",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Atur",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = Formatters.formatRupiah(totalExpense),
                                style = MonoMediumStyle,
                                color = budgetColor,
                            )
                            Text(
                                text = "${(budgetRatio * 100).toInt()}% terpakai",
                                style = MaterialTheme.typography.labelMedium,
                                color = budgetColor,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(budgetRatio.coerceIn(0f, 1f))
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(budgetColor)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            WalletExpenseChip(
                                label = "Dari Dompet Tunai",
                                amount = data.summary.cashExpense,
                                color = AmberCash,
                                isSelected = walletFilter == WalletType.CASH,
                                onClick = {
                                    walletFilter = if (walletFilter == WalletType.CASH) null else WalletType.CASH
                                },
                                modifier = Modifier.weight(1f),
                            )
                            WalletExpenseChip(
                                label = "Dari Transfer Bank",
                                amount = data.summary.bankExpense,
                                color = BankBlue,
                                isSelected = walletFilter == WalletType.BANK_TRANSFER,
                                onClick = {
                                    walletFilter = if (walletFilter == WalletType.BANK_TRANSFER) null else WalletType.BANK_TRANSFER
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Expense Category Donut & Bar Chart
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Grafik Pengeluaran per Kategori",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        ExpenseCategoryDonutChart(
                            slices = data.expenseByCategory,
                            totalExpense = data.summary.totalExpense,
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari judul pengeluaran, kategori, atau catatan...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_search_input"),
                )
            }

            // Wallet & Category Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = walletFilter == null,
                            onClick = { walletFilter = null },
                            label = { Text("Semua (${data.monthExpenses.size})") },
                        )
                        FilterChip(
                            selected = walletFilter == WalletType.CASH,
                            onClick = { walletFilter = WalletType.CASH },
                            label = { Text("Tunai") },
                        )
                        FilterChip(
                            selected = walletFilter == WalletType.BANK_TRANSFER,
                            onClick = { walletFilter = WalletType.BANK_TRANSFER },
                            label = { Text("Transfer Bank") },
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = categoryFilter == null,
                                onClick = { categoryFilter = null },
                                label = { Text("Semua Kategori") },
                                modifier = Modifier.testTag("expense_category_filter_all"),
                            )
                        }
                        items(ExpenseCategory.entries) { cat ->
                            FilterChip(
                                selected = categoryFilter == cat,
                                onClick = {
                                    categoryFilter = if (categoryFilter == cat) null else cat
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor(cat))
                                    )
                                },
                                label = { Text(cat.labelId) },
                                modifier = Modifier.testTag("expense_category_filter_${cat.code.lowercase()}"),
                            )
                        }
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (data.monthExpenses.isEmpty()) {
                                    "Belum ada pengeluaran pada periode ini."
                                } else {
                                    "Tidak ada pengeluaran sesuai filter/pencarian."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseRowCard(
                        expense = expense,
                        onClick = { onEditExpense(expense) },
                    )
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }

        ExtendedFloatingActionButton(
            onClick = onAddExpense,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.action_add_expense)) },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_expense_fab"),
        )
    }
}

@Composable
private fun WalletExpenseChip(
    label: String,
    amount: Long,
    color: Color,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.16f) else color.copy(alpha = 0.08f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) color else Color.Transparent,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = Formatters.formatRupiah(amount),
                style = MonoSmallStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExpenseRowCard(
    expense: Expense,
    onClick: () -> Unit,
) {
    val catColor = categoryColor(expense.expenseCategory)
    val walletColor = if (expense.wallet == WalletType.CASH) AmberCash else BankBlue

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("expense_card_${expense.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(catColor)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${expense.expenseCategory.labelId} • ${Formatters.formatDateFull(expense.expenseDateMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = walletColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = expense.wallet.labelId,
                        style = MaterialTheme.typography.labelSmall,
                        color = walletColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${Formatters.formatRupiah(expense.amount)}",
                    style = MonoSmallStyle,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonExpense,
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit pengeluaran",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}
