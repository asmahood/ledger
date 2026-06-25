package io.github.asmahood.ledger.data.repository

import io.github.asmahood.ledger.data.db.dao.TransactionDao
import io.github.asmahood.ledger.data.mapper.toEntity
import io.github.asmahood.ledger.data.mapper.toModel
import io.github.asmahood.ledger.data.model.MonthlyAmountStats
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.projection.PeriodTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

interface TransactionRepository {
    fun getAllTransactionsStream(): Flow<List<Transaction>>
    fun getTransactionStream(id: Long): Flow<Transaction?>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    fun getMonthlyAmountStatsStream(categoryId: Long): Flow<MonthlyAmountStats?>
    fun getPeriodTotalsStream(start: LocalDate, end: LocalDate): Flow<PeriodTotals>
}

class OfflineTransactionRepository @Inject constructor(private val dao: TransactionDao) :
    TransactionRepository {
    override fun getAllTransactionsStream(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { entities -> entities.map { it.toModel() } }
    }

    override fun getTransactionStream(id: Long): Flow<Transaction?> {
        return dao.getTransaction(id).map { it?.toModel() }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        dao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        dao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        dao.delete(transaction.toEntity())
    }

    override fun getMonthlyAmountStatsStream(categoryId: Long): Flow<MonthlyAmountStats?> {
        return dao.getMonthlyAmountStats(categoryId).map { it.toModel() }
    }

    override fun getPeriodTotalsStream(start: LocalDate, end: LocalDate): Flow<PeriodTotals> {
        return dao.getPeriodTotals(start, end)
    }
}