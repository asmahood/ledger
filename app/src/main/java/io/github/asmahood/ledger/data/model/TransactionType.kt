package io.github.asmahood.ledger.data.model

import androidx.annotation.StringRes
import io.github.asmahood.ledger.R

enum class TransactionType(@StringRes val label: Int) {

    EXPENSE(R.string.expense), INCOME(R.string.income);
}