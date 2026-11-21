package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.BillingStatus
import com.example.data.BusinessSettings
import com.example.data.Customer
import com.example.data.CustomerBillingItem
import com.example.data.Payment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DueReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val user = Firebase.auth.currentUser ?: return Result.success()
        if (!ReminderNotificationHelper.hasNotificationPermission(applicationContext)) {
            return Result.success()
        }

        return try {
            val databaseId = applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            val userDoc = db.collection("users").document(user.uid)

            val settingsSnap = userDoc.collection("settings").document("profile").get().await()
            val settings = if (settingsSnap.exists()) {
                settingsSnap.toObject(
                    BusinessSettings::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: BusinessSettings(userId = user.uid)
            } else {
                BusinessSettings(userId = user.uid)
            }

            if (!settings.remindersEnabled) {
                return Result.success()
            }

            val currentMonth = Formatters.currentYearMonth()
            val customersSnap = userDoc.collection("customers").get().await()
            val activeCustomers = customersSnap.toObjects(
                Customer::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).filter { it.active }

            if (activeCustomers.isEmpty()) {
                return Result.success()
            }

            val paymentsSnap = userDoc.collection("payments")
                .whereEqualTo("yearMonth", currentMonth)
                .get()
                .await()
            val monthPayments = paymentsSnap.toObjects(
                Payment::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val paymentByCustomerId = monthPayments.associateBy { it.customerId }
            val reminderWindow = settings.reminderDaysBefore.toInt().coerceIn(0, 15)

            val billingItems = activeCustomers.map { customer ->
                val payment = paymentByCustomerId[customer.id]
                val daysUntilDue = Formatters.daysUntilDueDate(currentMonth, customer.dueDay.toInt())
                val status = when {
                    payment != null -> BillingStatus.PAID
                    daysUntilDue < 0 -> BillingStatus.OVERDUE
                    daysUntilDue <= reminderWindow -> BillingStatus.DUE_SOON
                    else -> BillingStatus.UNPAID
                }
                CustomerBillingItem(
                    customer = customer,
                    payment = payment,
                    status = status,
                    daysUntilDue = daysUntilDue,
                )
            }

            val hasUrgent = billingItems.any {
                it.status == BillingStatus.OVERDUE || it.status == BillingStatus.DUE_SOON
            }
            if (hasUrgent) {
                ReminderNotificationHelper.showDueDateSummaryNotification(
                    context = applicationContext,
                    yearMonth = currentMonth,
                    billingItems = billingItems,
                )
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "DueReminderWorker check failed", e)
            if (runAttemptCount < 2) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val TAG = "DueReminderWorker"
        private const val UNIQUE_WORK_NAME = "netkas_daily_due_reminder"

        fun scheduleDailyCheck(context: Context) {
            val initialDelayMillis = computeDelayUntilNextMorning8Am()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DueReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancelDailyCheck(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
        }

        private fun computeDelayUntilNextMorning8Am(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(TimeUnit.MINUTES.toMillis(15))
        }
    }
}
