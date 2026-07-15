package io.github.asmahood.ledger.data.repository

import io.github.asmahood.ledger.data.model.MonthlyAmountStats
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.projection.CategoryMonthSpend
import io.github.asmahood.ledger.data.projection.PeriodTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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
    private val periodTotals = MutableStateFlow(PeriodTotals(income = null, expenses = null))
    private val categoryMonthlyTotals = MutableStateFlow<List<CategoryMonthSpend>>(emptyList())

    val inserted = mutableListOf<Transaction>()
    val updated = mutableListOf<Transaction>()
    val deleted = mutableListOf<Transaction>()
    val monthlyStatsRequestedFor = mutableListOf<Long>()
    val periodTotalsRequestedFor = mutableListOf<Pair<LocalDate, LocalDate>>()
    val categoryMonthlyTotalsRequestedFor = mutableListOf<Pair<LocalDate, LocalDate>>()

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

    fun setPeriodTotals(totals: PeriodTotals) {
        periodTotals.value = totals
    }

    fun setCategoryMonthlyTotals(totals: List<CategoryMonthSpend>) {
        categoryMonthlyTotals.value = totals
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

    override fun getPeriodTotalsStream(start: LocalDate, end: LocalDate): Flow<PeriodTotals> {
        periodTotalsRequestedFor += start to end
        return streamError?.let { error -> flow<PeriodTotals> { throw error } } ?: periodTotals
    }

    override fun getMonthlyCategoryTotalsStream(
        start: LocalDate,
        end: LocalDate
    ): Flow<List<CategoryMonthSpend>> {
        categoryMonthlyTotalsRequestedFor += start to end
        return streamError?.let { error -> flow { throw error } } ?: categoryMonthlyTotals
    }
}
