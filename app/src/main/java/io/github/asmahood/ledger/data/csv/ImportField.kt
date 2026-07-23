package io.github.asmahood.ledger.data.csv

import androidx.annotation.StringRes
import io.github.asmahood.ledger.R

/**
 * A field of [io.github.asmahood.ledger.data.model.Transaction] that a CSV column can be mapped to.
 *
 * [synonyms] are normalised header names (see [ColumnMapping.normalize]) that [ColumnMapping.autoDetect]
 * accepts for this field. Declaration order is the order the mapping step renders, and the order
 * auto-detection claims columns in.
 */
enum class ImportField(
    @StringRes val label: Int,
    val required: Boolean,
    val synonyms: Set<String>,
) {
    DATE(R.string.import_field_date, true, setOf("date", "transactiondate", "txndate", "posted", "posteddate", "day")),
    AMOUNT(R.string.import_field_amount, true, setOf("amount", "value", "total", "cost", "price")),
    CATEGORY(R.string.import_field_category, true, setOf("category", "cat", "categoryname", "group")),
    VENDOR(R.string.import_field_vendor, false, setOf("vendor", "merchant", "payee", "description", "store", "name")),
    TYPE(R.string.import_field_type, false, setOf("type", "transactiontype", "kind", "direction")),
    NOTE(R.string.import_field_note, false, setOf("note", "notes", "memo", "comment", "comments", "details"));
}
