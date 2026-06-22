package io.github.asmahood.ledger.ui.transaction.form

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class TransactionFormUiState(
    val id: Long = 0,
    val amount: String = "",
    val date: String = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
    val vendor: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val notes: String = "",
    val category: Category? = null,
    val categories: List<Category> = listOf(),
) {
    val isFormValid: Boolean
        get() {
            return try {
                val parsedAmount = amount.toDouble()
                amount.isNotBlank() && parsedAmount.isFinite() && parsedAmount > 0.0 && date.isNotBlank() && vendor.isNotBlank() && category != null
            } catch (e: NumberFormatException) {
                false
            }
        }

    val visibleCategories: List<Category>
        get() = categories.filter { it.type == type }
    fun toTransaction(): Transaction {
        return Transaction(
            id = id,
            amount = amount.toDouble(),
            date = LocalDate.parse(date, DateTimeFormatter.ofPattern("MM/dd/yyyy")),
            vendor = vendor,
            type = type,
            notes = notes,
            category = category!!
        )
    }
}
