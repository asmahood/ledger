package io.github.asmahood.ledger.ui.transaction.csvimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.csv.ParsedRow
import io.github.asmahood.ledger.data.csv.RowError
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.util.formatCurrency
import io.github.asmahood.ledger.util.transactionDateFormatter
import java.time.LocalDate

@Composable
fun ImportStepPreview(
    rows: List<ParsedRow>,
    duplicateLines: Set<Int>,
    selectedLines: Set<Int>,
    onRowToggled: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.import_preview_summary, selectedLines.size, rows.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.lineNumber }) { row ->
                when (row) {
                    is ParsedRow.Valid -> ValidRow(
                        row = row,
                        isDuplicate = row.lineNumber in duplicateLines,
                        isSelected = row.lineNumber in selectedLines,
                        onToggled = { onRowToggled(row.lineNumber) },
                    )

                    is ParsedRow.Invalid -> InvalidRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun ValidRow(
    row: ParsedRow.Valid,
    isDuplicate: Boolean,
    isSelected: Boolean,
    onToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 12.dp),
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggled() })
            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(
                    text = row.vendor.ifBlank { row.categoryName },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${row.date.format(transactionDateFormatter)} · ${row.categoryName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isDuplicate) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.import_badge_duplicate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = formatCurrency(row.amount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (row.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InvalidRow(row: ParsedRow.Invalid, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 12.dp),
        ) {
            Checkbox(checked = false, onCheckedChange = null, enabled = false)
            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(
                    text = row.raw.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(row.error.message),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun ImportResultSummary(result: ImportResult, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.import_result_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        ResultLine(R.string.import_result_imported, result.imported, alwaysShow = true)
        ResultLine(R.string.import_result_invalid, result.invalid)
        ResultLine(R.string.import_result_duplicates, result.duplicatesSkipped)
        ResultLine(R.string.import_result_category_skipped, result.categorySkipped)
        ResultLine(R.string.import_result_categories_created, result.categoriesCreated)
    }
}

@Composable
private fun ResultLine(labelRes: Int, count: Int, alwaysShow: Boolean = false) {
    if (count == 0 && !alwaysShow) return
    Text(
        text = stringResource(labelRes, count),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewLightDark
@Composable
private fun ImportStepPreviewPreview() {
    val jan5 = LocalDate.of(2026, 1, 5)
    LedgerTheme {
        ImportStepPreview(
            rows = listOf(
                ParsedRow.Valid(2, jan5, 12.50, "Groceries", "Food Basics", TransactionType.EXPENSE, null),
                ParsedRow.Valid(3, jan5, 4.25, "Coffee", "Blue Bottle", TransactionType.EXPENSE, null),
                ParsedRow.Invalid(4, RowError.UNPARSEABLE_DATE, listOf("nope", "9.99", "Groceries")),
            ),
            duplicateLines = setOf(3),
            selectedLines = setOf(2, 3),
            onRowToggled = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ImportResultSummaryPreview() {
    LedgerTheme {
        ImportResultSummary(
            result = ImportResult(
                imported = 308,
                invalid = 4,
                duplicatesSkipped = 2,
                categorySkipped = 0,
                categoriesCreated = 1,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}