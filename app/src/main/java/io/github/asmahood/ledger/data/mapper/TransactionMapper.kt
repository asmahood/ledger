package io.github.asmahood.ledger.data.mapper

import io.github.asmahood.ledger.data.db.entity.TransactionEntity
import io.github.asmahood.ledger.data.db.relation.TransactionWithCategory
import io.github.asmahood.ledger.data.model.MonthlyAmountStats
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionDayGroup
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.data.projection.CategoryMonthlyAmountStats

fun TransactionWithCategory.toModel(): Transaction {
    return Transaction(
        id = transaction.id,
        amount = transaction.amount,
        date = transaction.date,
        vendor = transaction.vendor,
        type = TransactionType.valueOf(transaction.type),
        notes = transaction.notes,
        category = category.toModel()
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount,
        date = date,
        vendor = vendor,
        type = type.toString(),
        notes = notes,
        categoryId = category.id
    )
}

fun List<Transaction>.toDayGroups(): List<TransactionDayGroup> =
    groupBy { it.date }.map { (date, transactions) -> TransactionDayGroup(date, transactions) }

fun CategoryMonthlyAmountStats.toModel(): MonthlyAmountStats? {
    if (average == null || minimum == null || maximum == null) {
        return null
    }

    return MonthlyAmountStats(
        average = average,
        minimum = minimum,
        maximum = maximum
    )
}