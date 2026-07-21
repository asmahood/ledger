package io.github.asmahood.ledger.data.projection

data class MonthlyTotal(
    val year: Int,
    val month: Int,
    val total: Double
)