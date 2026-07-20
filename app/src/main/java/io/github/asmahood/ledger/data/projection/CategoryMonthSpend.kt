package io.github.asmahood.ledger.data.projection

data class CategoryMonthSpend(
    val categoryId: Long,
    val categoryName: String,
    val year: Int,
    val month: Int,
    val total: Double
)