package io.github.asmahood.ledger.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.asmahood.ledger.data.db.entity.TransactionEntity
import io.github.asmahood.ledger.data.db.relation.TransactionWithCategory
import io.github.asmahood.ledger.data.projection.CategoryMonthSpend
import io.github.asmahood.ledger.data.projection.CategoryMonthlyAmountStats
import io.github.asmahood.ledger.data.projection.PeriodTotals
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransaction(id: Long): Flow<TransactionWithCategory?>

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionWithCategory>>

    @Query("""
        SELECT  SUM(monthly_total) / (MAX(month_index) - MIN(month_index) + 1) AS average,
                MIN(monthly_total) AS minimum,
                MAX(monthly_total) AS maximum
        FROM (
            SELECT  SUM(amount) AS monthly_total,
                    CAST(strftime('%Y', date * 86400, 'unixepoch') AS INTEGER) * 12
                        + CAST(strftime('%m', date * 86400, 'unixepoch') AS INTEGER) AS month_index
            FROM transactions
            WHERE category_id = :categoryId
            GROUP BY month_index
        )
    """)
    fun getMonthlyAmountStats(categoryId: Long): Flow<CategoryMonthlyAmountStats>

    @Query("""
        SELECT
            SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) AS income,
            SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expenses
        FROM transactions
        WHERE date BETWEEN :start AND :end
    """)
    fun getPeriodTotals(start: LocalDate, end: LocalDate): Flow<PeriodTotals>

    @Query("""
        SELECT 
            c.id AS categoryId,
            c.name AS categoryName,
            CAST(strftime('%Y', t.date * 86400, 'unixepoch') AS INTEGER) AS year,
            CAST(strftime('%m', t.date * 86400, 'unixepoch') AS INTEGER) AS month,
            SUM(t.amount) AS total
        FROM transactions t
        INNER JOIN categories c ON c.id = t.category_id
        WHERE t.type = 'EXPENSE' AND t.date BETWEEN :start AND :end
        GROUP BY c.id, year, month
        ORDER BY year, month, c.name
    """)
    fun getMonthlyCategoryTotals(start: LocalDate, end: LocalDate): Flow<List<CategoryMonthSpend>>
}