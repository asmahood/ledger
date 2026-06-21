package io.github.asmahood.ledger.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "budgets",
    primaryKeys = ["category_id"],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
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
