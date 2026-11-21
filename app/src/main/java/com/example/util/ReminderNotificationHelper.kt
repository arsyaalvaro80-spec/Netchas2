package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BillingStatus
import com.example.data.CustomerBillingItem

object ReminderNotificationHelper {
    private const val CHANNEL_ID = "netkas_billing_reminders"
    private const val NOTIF_ID_DUE = 20261

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showDueDateSummaryNotification(
        context: Context,
        yearMonth: String,
        billingItems: List<CustomerBillingItem>,
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)

        val urgentItems = billingItems.filter {
            it.status == BillingStatus.OVERDUE || it.status == BillingStatus.DUE_SOON
        }
        val unpaidItems = billingItems.filter { it.payment == null }

        val title: String
        val body: String
        val bigText: String

        if (urgentItems.isNotEmpty()) {
            val totalUrgent = urgentItems.sumOf { it.customer.monthlyFee }
            title = "Pengingat Jatuh Tempo Wi-Fi (${urgentItems.size} Pelanggan)"
            body = "Total ${Formatters.formatRupiah(totalUrgent)} perlu segera ditagih agar arus kas lancar."
            val preview = urgentItems.take(5).joinToString("\n") { item ->
                val statusTag = when {
                    item.status == BillingStatus.OVERDUE -> "Terlambat ${-item.daysUntilDue} hari"
                    item.daysUntilDue == 0 -> "Hari Ini"
                    else -> "H-${item.daysUntilDue}"
                }
                "• ${item.customer.name} (${Formatters.formatRupiah(item.customer.monthlyFee)}) — $statusTag"
            }
            bigText = "$body\n\n$preview"
        } else if (unpaidItems.isNotEmpty()) {
            val totalUnpaid = unpaidItems.sumOf { it.customer.monthlyFee }
            title = "Tagihan ${Formatters.formatYearMonthFull(yearMonth)}: ${unpaidItems.size} Belum Lunas"
            body = "Sisa tagihan ${Formatters.formatRupiah(totalUnpaid)} belum jatuh tempo."
            bigText = body
        } else {
            title = "Arus Kas Aman • ${Formatters.formatYearMonthFull(yearMonth)}"
            body = "Seluruh pelanggan Wi-Fi aktif sudah melunasi tagihan bulan ini!"
            bigText = body
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_DUE, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
