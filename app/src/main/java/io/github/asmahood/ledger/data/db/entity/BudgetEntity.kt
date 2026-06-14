package io.github.asmahood.ledger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "budgets", primaryKeys = ["category_id"])
data class BudgetEntity(
    /**
     * The category this budget corresponds to
     */
    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    /**
     * The total monthly amount for this budget
     */
    @ColumnInfo(name = "monthly_amount")
    val monthlyAmount: Double
)
