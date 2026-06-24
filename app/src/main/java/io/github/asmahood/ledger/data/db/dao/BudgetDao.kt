package io.github.asmahood.ledger.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.asmahood.ledger.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: Long)

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId")
    fun getByCategory(categoryId: Long): Flow<BudgetEntity?>
}