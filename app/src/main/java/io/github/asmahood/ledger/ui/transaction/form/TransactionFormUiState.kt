package io.github.asmahood.ledger.ui.transaction.form

import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.util.transactionDateFormatter
import java.time.LocalDate

data class TransactionFormUiState(
    val id: Long = 0,
    val amount: String = "",
    val date: String = LocalDate.now().format(transactionDateFormatter),
    val vendor: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val notes: String = "",
    val category: Category? = null,
    val categories: List<Category> = listOf(),
    val isEditMode: Boolean = false,
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
            date = LocalDate.parse(date, transactionDateFormatter),
            vendor = vendor,
            type = type,
            notes = notes,
            category = category!!
        )
    }
}
