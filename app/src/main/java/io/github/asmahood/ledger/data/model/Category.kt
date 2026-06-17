package io.github.asmahood.ledger.data.model

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val description: String? = null
)
