package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NetKasRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Silakan masuk dengan akun Google terlebih dahulu.")
        check(uid == expectedUserId) { "Sesi pengguna berubah; operasi dibatalkan." }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // --- OBSERVE STREAMS (Real-time snapshots with ServerTimestampBehavior.ESTIMATE) ---

    fun observeSettings(userId: String): Flow<BusinessSettings> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/settings/profile"
        emitAll(
            userDoc(uid).collection("settings").document("profile")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            BusinessSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: BusinessSettings(userId = uid)
                    } else {
                        BusinessSettings(userId = uid)
                    }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeCustomers(userId: String): Flow<List<Customer>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/customers"
        emitAll(
            userDoc(uid).collection("customers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Customer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePayments(userId: String): Flow<List<Payment>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/payments"
        emitAll(
            userDoc(uid).collection("payments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Payment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.paidDateMillis }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeExpenses(userId: String): Flow<List<Expense>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/expenses"
        emitAll(
            userDoc(uid).collection("expenses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Expense::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.expenseDateMillis }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeWalletTransfers(userId: String): Flow<List<WalletTransfer>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/wallet_transfers"
        emitAll(
            userDoc(uid).collection("wallet_transfers")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WalletTransfer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.transferDateMillis }
                }
                .catch { error ->
                    logStreamErrorIfSignedIn(error, uid, OperationType.LIST, path)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfSignedIn(
        error: Throwable,
        uid: String,
        op: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    // --- SETTINGS MUTATION ---

    suspend fun saveBusinessSettings(
        userId: String,
        businessName: String,
        businessPhone: String,
        businessAddress: String,
        bankAccountInfo: String,
        monthlyExpenseBudget: Long,
        reminderDaysBefore: Long,
        remindersEnabled: Boolean,
    ): Result<Unit> {
        val path = "users/$userId/settings/profile"
        return try {
            val uid = requireUserId(userId)
            val cleanName = businessName.trim().ifEmpty { "NetKas Wi-Fi" }.take(80)
            val cleanPhone = businessPhone.trim().take(30)
            val cleanAddress = businessAddress.trim().take(180)
            val cleanBank = bankAccountInfo.trim().take(180)
            val cleanBudget = monthlyExpenseBudget.coerceIn(0L, 10_000_000_000L)
            val cleanDays = reminderDaysBefore.coerceIn(0L, 15L)

            val docRef = userDoc(uid).collection("settings").document("profile")
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "businessName" to cleanName,
                        "businessPhone" to cleanPhone,
                        "businessAddress" to cleanAddress,
                        "bankAccountInfo" to cleanBank,
                        "monthlyExpenseBudget" to cleanBudget,
                        "reminderDaysBefore" to cleanDays,
                        "remindersEnabled" to remindersEnabled,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "businessName" to cleanName,
                        "businessPhone" to cleanPhone,
                        "businessAddress" to cleanAddress,
                        "bankAccountInfo" to cleanBank,
                        "monthlyExpenseBudget" to cleanBudget,
                        "reminderDaysBefore" to cleanDays,
                        "remindersEnabled" to remindersEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- CUSTOMER MUTATIONS ---

    suspend fun saveCustomer(
        userId: String,
        existingCustomerId: String?,
        name: String,
        phone: String,
        email: String,
        address: String,
        packageName: String,
        monthlyFee: Long,
        dueDay: Long,
        active: Boolean,
        notes: String,
    ): Result<String> {
        var path = "users/$userId/customers"
        return try {
            val uid = requireUserId(userId)
            val cleanName = name.trim().take(100)
            require(cleanName.isNotEmpty()) { "Nama pelanggan wajib diisi." }
            val cleanPkg = packageName.trim().take(60)
            require(cleanPkg.isNotEmpty()) { "Nama paket Wi-Fi wajib diisi." }
            require(monthlyFee in 1L..100_000_000L) { "Tarif bulanan harus antara Rp 1 hingga Rp 100.000.000." }
            val cleanDueDay = dueDay.coerceIn(1L, 28L)

            val collection = userDoc(uid).collection("customers")
            if (existingCustomerId.isNullOrBlank()) {
                val docRef = collection.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "phone" to phone.trim().take(30),
                        "email" to email.trim().take(120),
                        "address" to address.trim().take(200),
                        "packageName" to cleanPkg,
                        "monthlyFee" to monthlyFee,
                        "dueDay" to cleanDueDay,
                        "active" to active,
                        "notes" to notes.trim().take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                Result.success(docRef.id)
            } else {
                val docRef = collection.document(existingCustomerId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "phone" to phone.trim().take(30),
                        "email" to email.trim().take(120),
                        "address" to address.trim().take(200),
                        "packageName" to cleanPkg,
                        "monthlyFee" to monthlyFee,
                        "dueDay" to cleanDueDay,
                        "active" to active,
                        "notes" to notes.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                Result.success(existingCustomerId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingCustomerId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                path
            )
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(userId: String, customerId: String): Result<Unit> {
        val path = "users/$userId/customers/$customerId"
        return try {
            val uid = requireUserId(userId)
            require(customerId.isNotBlank()) { "ID pelanggan tidak valid." }
            userDoc(uid).collection("customers").document(customerId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- PAYMENT MUTATIONS (Collision-safe across period moves, returns saved Payment for instant receipt) ---

    suspend fun recordOrUpdatePayment(
        userId: String,
        existingPaymentId: String?,
        customerId: String,
        customerName: String,
        packageName: String,
        yearMonth: String,
        amount: Long,
        paymentMethod: WalletType,
        bankReference: String,
        receiptNumber: String,
        paidDateMillis: Long,
        notes: String,
    ): Result<Payment> {
        var path = "users/$userId/payments"
        return try {
            val uid = requireUserId(userId)
            require(customerId.isNotBlank()) { "Pelanggan wajib dipilih." }
            require(YEAR_MONTH_REGEX.matches(yearMonth)) { "Format periode bulan tidak valid (YYYY-MM)." }
            require(amount in 1L..100_000_000L) { "Nominal pembayaran harus lebih dari Rp 0." }
            val cleanCustomerName = customerName.trim().take(100).ifEmpty { "Pelanggan" }
            val cleanPkg = packageName.trim().take(60).ifEmpty { "Paket Wi-Fi" }
            val cleanBankRef = bankReference.trim().take(120)
            val cleanNotes = notes.trim().take(300)
            val cleanReceipt = receiptNumber.trim().take(50).ifEmpty {
                generateReceiptNumber(yearMonth, customerId)
            }
            val cleanPaidMillis = paidDateMillis.coerceAtLeast(0L)

            val collection = userDoc(uid).collection("payments")
            val canonicalId = "${customerId}_${yearMonth}"
            val isMovingPeriod = !existingPaymentId.isNullOrBlank() && existingPaymentId != canonicalId
            val docRef = collection.document(canonicalId)
            path = docRef.path

            if (isMovingPeriod) {
                val conflictSnap = docRef.get().await()
                if (conflictSnap.exists()) {
                    throw IllegalStateException("Pelanggan ini sudah memiliki catatan pembayaran pada bulan tujuan.")
                }
                val oldRef = collection.document(existingPaymentId!!)
                val batch = db.batch()
                batch.set(
                    docRef,
                    mapOf(
                        "userId" to uid,
                        "customerId" to customerId,
                        "customerName" to cleanCustomerName,
                        "packageName" to cleanPkg,
                        "yearMonth" to yearMonth,
                        "amount" to amount,
                        "paymentMethod" to paymentMethod.code,
                        "bankReference" to cleanBankRef,
                        "receiptNumber" to cleanReceipt,
                        "paidDateMillis" to cleanPaidMillis,
                        "notes" to cleanNotes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                batch.delete(oldRef)
                batch.commit().await()
            } else {
                val existing = docRef.get().await()
                if (existing.exists()) {
                    if (existingPaymentId.isNullOrBlank()) {
                        throw IllegalStateException("Pelanggan ini sudah melunasi tagihan bulan $yearMonth. Gunakan tombol Edit untuk mengubah data.")
                    }
                    docRef.update(
                        mapOf(
                            "customerName" to cleanCustomerName,
                            "packageName" to cleanPkg,
                            "yearMonth" to yearMonth,
                            "amount" to amount,
                            "paymentMethod" to paymentMethod.code,
                            "bankReference" to cleanBankRef,
                            "receiptNumber" to cleanReceipt,
                            "paidDateMillis" to cleanPaidMillis,
                            "notes" to cleanNotes,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "customerId" to customerId,
                            "customerName" to cleanCustomerName,
                            "packageName" to cleanPkg,
                            "yearMonth" to yearMonth,
                            "amount" to amount,
                            "paymentMethod" to paymentMethod.code,
                            "bankReference" to cleanBankRef,
                            "receiptNumber" to cleanReceipt,
                            "paidDateMillis" to cleanPaidMillis,
                            "notes" to cleanNotes,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                }
            }

            Result.success(
                Payment(
                    id = canonicalId,
                    userId = uid,
                    customerId = customerId,
                    customerName = cleanCustomerName,
                    packageName = cleanPkg,
                    yearMonth = yearMonth,
                    amount = amount,
                    paymentMethod = paymentMethod.code,
                    bankReference = cleanBankRef,
                    receiptNumber = cleanReceipt,
                    paidDateMillis = cleanPaidMillis,
                    notes = cleanNotes,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePayment(userId: String, paymentId: String): Result<Unit> {
        val path = "users/$userId/payments/$paymentId"
        return try {
            val uid = requireUserId(userId)
            require(paymentId.isNotBlank()) { "ID pembayaran tidak valid." }
            userDoc(uid).collection("payments").document(paymentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- EXPENSE MUTATIONS ---

    suspend fun saveExpense(
        userId: String,
        existingExpenseId: String?,
        yearMonth: String,
        title: String,
        category: ExpenseCategory,
        amount: Long,
        walletType: WalletType,
        expenseDateMillis: Long,
        notes: String,
    ): Result<String> {
        var path = "users/$userId/expenses"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Judul pengeluaran wajib diisi." }
            require(YEAR_MONTH_REGEX.matches(yearMonth)) { "Format periode bulan tidak valid (YYYY-MM)." }
            require(amount in 1L..100_000_000L) { "Nominal pengeluaran harus lebih dari Rp 0." }
            val cleanMillis = expenseDateMillis.coerceAtLeast(0L)

            val collection = userDoc(uid).collection("expenses")
            if (existingExpenseId.isNullOrBlank()) {
                val docRef = collection.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "yearMonth" to yearMonth,
                        "title" to cleanTitle,
                        "category" to category.code,
                        "amount" to amount,
                        "walletType" to walletType.code,
                        "expenseDateMillis" to cleanMillis,
                        "notes" to notes.trim().take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                Result.success(docRef.id)
            } else {
                val docRef = collection.document(existingExpenseId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "yearMonth" to yearMonth,
                        "title" to cleanTitle,
                        "category" to category.code,
                        "amount" to amount,
                        "walletType" to walletType.code,
                        "expenseDateMillis" to cleanMillis,
                        "notes" to notes.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                Result.success(existingExpenseId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingExpenseId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                path
            )
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> {
        val path = "users/$userId/expenses/$expenseId"
        return try {
            val uid = requireUserId(userId)
            require(expenseId.isNotBlank()) { "ID pengeluaran tidak valid." }
            userDoc(uid).collection("expenses").document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- WALLET TRANSFER MUTATIONS (Setor Tunai ke Bank / Tarik Tunai) ---

    suspend fun saveWalletTransfer(
        userId: String,
        existingTransferId: String?,
        yearMonth: String,
        fromWallet: WalletType,
        toWallet: WalletType,
        amount: Long,
        transferDateMillis: Long,
        notes: String,
    ): Result<String> {
        var path = "users/$userId/wallet_transfers"
        return try {
            val uid = requireUserId(userId)
            require(fromWallet != toWallet) { "Dompet asal dan tujuan tidak boleh sama." }
            require(YEAR_MONTH_REGEX.matches(yearMonth)) { "Format periode bulan tidak valid (YYYY-MM)." }
            require(amount in 1L..100_000_000L) { "Nominal pindah saldo harus lebih dari Rp 0." }
            val cleanMillis = transferDateMillis.coerceAtLeast(0L)
            val cleanNotes = notes.trim().take(300)

            val collection = userDoc(uid).collection("wallet_transfers")
            if (existingTransferId.isNullOrBlank()) {
                val docRef = collection.document()
                path = docRef.path
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "yearMonth" to yearMonth,
                        "fromWallet" to fromWallet.code,
                        "toWallet" to toWallet.code,
                        "amount" to amount,
                        "transferDateMillis" to cleanMillis,
                        "notes" to cleanNotes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                Result.success(docRef.id)
            } else {
                val docRef = collection.document(existingTransferId)
                path = docRef.path
                docRef.update(
                    mapOf(
                        "yearMonth" to yearMonth,
                        "fromWallet" to fromWallet.code,
                        "toWallet" to toWallet.code,
                        "amount" to amount,
                        "transferDateMillis" to cleanMillis,
                        "notes" to cleanNotes,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                Result.success(existingTransferId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (existingTransferId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE,
                path
            )
            Result.failure(e)
        }
    }

    suspend fun deleteWalletTransfer(userId: String, transferId: String): Result<Unit> {
        val path = "users/$userId/wallet_transfers/$transferId"
        return try {
            val uid = requireUserId(userId)
            require(transferId.isNotBlank()) { "ID pindah saldo tidak valid." }
            userDoc(uid).collection("wallet_transfers").document(transferId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        private val YEAR_MONTH_REGEX = Regex("^[0-9]{4}-(0[1-9]|1[0-2])$")

        fun generateReceiptNumber(yearMonth: String, customerId: String): String {
            val ymCompact = yearMonth.replace("-", "")
            val suffix = customerId.takeLast(4).uppercase().ifEmpty { "0001" }
            val timePart = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
            return "NK-$ymCompact-$suffix-$timePart"
        }
    }
}
