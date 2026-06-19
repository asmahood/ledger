package io.github.asmahood.ledger.util

import java.text.NumberFormat

fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance().format(amount)
}