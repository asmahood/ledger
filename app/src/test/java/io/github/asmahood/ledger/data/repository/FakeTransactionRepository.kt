package io.github.asmahood.ledger.data.repository

import io.github.asmahood.ledger.data.model.MonthlyAmountStats
import io.github.asmahood.ledger.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Controllable fake for [TransactionRepository] used in ViewModel unit tests.
 *
 * - Drive the transactions stream with [setTransactions].
 * - Make the stream fail by setting [streamError] (covers ViewModel error paths).
 * - Inspect writes via [inserted] / [updated] / [deleted].
 * - Drive the per-category monthly stats with [setMonthlyStats]; inspect which
 *   categories were queried via [monthlyStatsRequestedFor].
 */
class FakeTransactionRepository : TransactionRepository {
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val monthlyStats = MutableStateFlow<Map<Long, MonthlyAmountStats?>>(emptyMap())

    val inserted = mutableListOf<Transaction>()
    val updated = mutableListOf<Transaction>()
    val deleted = mutableListOf<Transaction>()
    val monthlyStatsRequestedFor = mutableListOf<Long>()

    var streamError: Throwable? = null
    var insertError: Throwable? = null
    var updateError: Throwable? = null
    var deleteError: Throwable? = null

    fun setTransactions(values: List<Transaction>) {
        transactions.value = values
    }

    fun setMonthlyStats(categoryId: Long, stats: MonthlyAmountStats?) {
        monthlyStats.value = monthlyStats.value + (categoryId to stats)
    }

    override fun getAllTransactionsStream(): Flow<List<Transaction>> =
        streamError?.let { error -> flow<List<Transaction>> { throw error } } ?: transactions

    override fun getTransactionStream(id: Long): Flow<Transaction?> =
        streamError?.let { error -> flow<Transaction?> { throw error } }
            ?: transactions.map { list -> list.find { it.id == id } }

    override suspend fun insertTransaction(transaction: Transaction) {
        insertError?.let { throw it }
        inserted += transaction
        transactions.value = transactions.value + transaction
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        updateError?.let { throw it }
        updated += transaction
        transactions.value = transactions.value.map { if (it.id == transaction.id) transaction else it }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        deleteError?.let { throw it }
        deleted += transaction
        transactions.value = transactions.value.filterNot { it.id == transaction.id }
    }

    override fun getMonthlyAmountStatsStream(categoryId: Long): Flow<MonthlyAmountStats?> {
        monthlyStatsRequestedFor += categoryId
        return monthlyStats.map { it[categoryId] }
    }
}
