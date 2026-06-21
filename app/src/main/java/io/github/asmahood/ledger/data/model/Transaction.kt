package io.github.asmahood.ledger.data.model

import java.time.LocalDate

data class Transaction(
    val id: Long,
    val amount: Double,
    val date: LocalDate,
    val vendor: String,
    val type: TransactionType,
    val notes: String?,
    val category: Category
)