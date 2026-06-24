package io.github.asmahood.ledger.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import io.github.asmahood.ledger.data.db.entity.BudgetEntity
import io.github.asmahood.ledger.data.db.entity.CategoryEntity

data class CategoryWithBudget(
    @Embedded
    val category: CategoryEntity,

    @Relation(parentColumn = "id", entityColumn = "category_id")
    val budget: BudgetEntity?
)
