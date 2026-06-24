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
import io.github.asmahood.ledger.data.projection.CategoryMonthlyAmountStats
import kotlinx.coroutines.flow.Flow

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
}