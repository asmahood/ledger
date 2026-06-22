package io.github.asmahood.ledger.util

import java.text.NumberFormat
import java.time.format.DateTimeFormatter

/** Display/parse format for transaction dates (e.g. 06/21/2026). */
val transactionDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance().format(amount)
}