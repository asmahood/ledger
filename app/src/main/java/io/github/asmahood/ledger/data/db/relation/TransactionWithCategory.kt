package io.github.asmahood.ledger.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import io.github.asmahood.ledger.data.db.entity.CategoryEntity
import io.github.asmahood.ledger.data.db.entity.TransactionEntity

data class TransactionWithCategory(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity
)
