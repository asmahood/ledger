package io.github.asmahood.ledger.data.csv

import androidx.annotation.StringRes
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import kotlin.math.abs

enum class RowError(@StringRes val message: Int) {
    MISSING_DATE(R.string.import_error_missing_date),
    UNPARSEABLE_DATE(R.string.import_error_unparseable_date),
    MISSING_AMOUNT(R.string.import_error_missing_amount),
    UNPARSEABLE_AMOUNT(R.string.import_error_unparseable_amount),
    ZERO_AMOUNT(R.string.import_error_zero_amount),
    MISSING_CATEGORY(R.string.import_error_missing_category),
    UNKNOWN_TYPE(R.string.import_error_unknown_type),
}

sealed interface ParsedRow {
    val lineNumber: Int

    data class Valid(
        override val lineNumber: Int,
        val date: LocalDate,
        val amount: Double,
        val categoryName: String,
        val vendor: String,
        val type: TransactionType,
        val note: String?,
    ) : ParsedRow

    data class Invalid(
        override val lineNumber: Int,
        val error: RowError,
        val raw: List<String>,
    ) : ParsedRow
}

object RowParser {

    /**
     * Tried in order. Year-first patterns come first so an ISO date is never reinterpreted as
     * month-first. Day-first (`31-12-2026`, `31/12/2026`) is deliberately absent — it cannot be
     * told apart from month-first for days 1-12, and a silently wrong date is worse than a
     * flagged row.
     */
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("uuuu-MM-dd"),
        DateTimeFormatter.ofPattern("uuuu/MM/dd"),
        DateTimeFormatter.ofPattern("M/d/uuuu"),
        DateTimeFormatter.ofPattern("M-d-uuuu"),
    ).map { it.withResolverStyle(ResolverStyle.STRICT) }

    fun parse(row: List<String>, mapping: ColumnMapping, lineNumber: Int): ParsedRow {
        val dateCell = row.cell(mapping, ImportField.DATE)
        val date = if (dateCell.isBlank()) {
            return ParsedRow.Invalid(lineNumber, RowError.MISSING_DATE, row)
        } else {
            parseDate(dateCell) ?: return ParsedRow.Invalid(lineNumber, RowError.UNPARSEABLE_DATE, row)
        }

        val amountCell = row.cell(mapping, ImportField.AMOUNT)
        val signedAmount = if (amountCell.isBlank()) {
            return ParsedRow.Invalid(lineNumber, RowError.MISSING_AMOUNT, row)
        } else {
            parseAmount(amountCell) ?: return ParsedRow.Invalid(lineNumber, RowError.UNPARSEABLE_AMOUNT, row)
        }
        if (signedAmount == 0.0) {
            return ParsedRow.Invalid(lineNumber, RowError.ZERO_AMOUNT, row)
        }

        val categoryName = row.cell(mapping, ImportField.CATEGORY)
        if (categoryName.isBlank()) {
            return ParsedRow.Invalid(lineNumber, RowError.MISSING_CATEGORY, row)
        }

        val typeCell = row.cell(mapping, ImportField.TYPE)
        val type = if (typeCell.isBlank()) {
            if (signedAmount < 0) TransactionType.EXPENSE else TransactionType.INCOME
        } else {
            parseType(typeCell) ?: return ParsedRow.Invalid(lineNumber, RowError.UNKNOWN_TYPE, row)
        }

        val vendor = row.cell(mapping, ImportField.VENDOR)
        val note = row.cell(mapping, ImportField.NOTE).ifBlank { null }

        return ParsedRow.Valid(
            lineNumber = lineNumber,
            date = date,
            amount = abs(signedAmount),
            categoryName = categoryName,
            vendor = vendor,
            type = type,
            note = note,
        )
    }

    fun parseDate(raw: String): LocalDate? {
        val trimmed = raw.trim()
        for (formatter in dateFormats) {
            try {
                return LocalDate.parse(trimmed, formatter)
            } catch (_: DateTimeParseException) {
                // try the next format
            }
        }
        return null
    }

    fun parseAmount(raw: String): Double? {
        val trimmed = raw.trim()
        val negative = trimmed.startsWith("-") || (trimmed.startsWith("(") && trimmed.endsWith(")"))
        val digits = trimmed.filter { it.isDigit() || it == '.' }
        val value = digits.toDoubleOrNull() ?: return null
        return if (negative) -value else value
    }

    fun parseType(raw: String): TransactionType? {
        return when (raw.trim().lowercase()) {
            "expense", "debit" -> TransactionType.EXPENSE
            "income", "credit" -> TransactionType.INCOME
            else -> null
        }
    }

    private fun List<String>.cell(mapping: ColumnMapping, field: ImportField): String {
        return mapping[field]?.let { getOrNull(it) }?.trim().orEmpty()
    }
}
