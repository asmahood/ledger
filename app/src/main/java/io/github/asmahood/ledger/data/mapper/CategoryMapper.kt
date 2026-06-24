package io.github.asmahood.ledger.data.mapper

import io.github.asmahood.ledger.data.db.entity.CategoryEntity
import io.github.asmahood.ledger.data.db.relation.CategoryWithBudget
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType

fun CategoryEntity.toModel(): Category {
    return Category(
        id = this.id,
        name = this.name,
        type = TransactionType.valueOf(this.type),
        description = this.description,
        budget = null
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = this.id,
        name = this.name,
        type = this.type.toString(),
        description = this.description
    )
}

fun CategoryWithBudget.toModel(): Category {
    return Category(
        id = category.id,
        name = category.name,
        type = TransactionType.valueOf(category.type),
        description = category.description,
        budget = budget?.monthlyAmount
    )
}