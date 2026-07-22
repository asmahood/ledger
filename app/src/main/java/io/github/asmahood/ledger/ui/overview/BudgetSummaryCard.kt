package io.github.asmahood.ledger.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.ui.theme.LocalChartColors
import io.github.asmahood.ledger.ui.theme.figureStyle
import io.github.asmahood.ledger.util.formatCurrency

/** Identifies the spending-vs-budget card in UI tests, independent of any user-visible label. */
internal const val BudgetSummaryTestTag = "budget_summary_card"

/**
 * Actual spend against period-scaled budget targets, split into budgeted and unbudgeted expense
 * categories. Every number and colour is decided in [buildBudgetSummary]; this renders what it is
 * given.
 */
@Composable
internal fun BudgetSummaryCard(
    summary: BudgetSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag(BudgetSummaryTestTag)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SummaryEyebrow(stringResource(R.string.spending_vs_budget))

            BudgetSubsection(
                label = stringResource(R.string.budgeted),
                rows = summary.budgeted,
                total = summary.budgetedTotal,
                modifier = Modifier.padding(top = 20.dp)
            )

            BudgetSubsection(
                label = stringResource(R.string.unbudgeted),
                rows = summary.unbudgeted,
                total = summary.unbudgetedTotal,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

/**
 * One labelled group of category rows closed by its own total. Renders nothing when the group has
 * neither rows nor a target to report, so a user with no budgets set doesn't meet an empty heading.
 */
@Composable
private fun BudgetSubsection(
    label: String,
    rows: List<BudgetRow>,
    total: BudgetRow,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty() && total.target == 0.0) return

    Column(modifier = modifier.fillMaxWidth()) {
        // The rule running off the label reads as a section break without adding a second type size.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }

        rows.forEach { row ->
            BudgetRowItem(row = row, modifier = Modifier.padding(top = 16.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
        BudgetRowItem(
            row = total,
            label = stringResource(R.string.budget_total),
            isTotal = true,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

/**
 * A category name, its `$actual/$target (%)` figures, and the bar beneath. Totals carry no bar —
 * a summed row has no single budget to fill.
 */
@Composable
private fun BudgetRowItem(
    row: BudgetRow,
    modifier: Modifier = Modifier,
    label: String = row.label,
    isTotal: Boolean = false,
) {
    val statusColor = statusColor(row.status)
    val amounts = stringResource(
        R.string.budget_amount_of,
        formatCurrency(row.actual),
        formatCurrency(row.target)
    )
    val percent = row.percent
        ?.let { stringResource(R.string.budget_percent, it) }
        ?: stringResource(R.string.budget_percent_unknown)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isTotal) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 12.dp)
            )
            Text(
                text = "$amounts $percent",
                style = figureStyle(MaterialTheme.typography.bodyMedium),
                fontWeight = if (isTotal) FontWeight.SemiBold else FontWeight.Normal,
                // Overspending is the one thing worth shouting about, so only it tints the figures.
                color = if (row.status == BudgetStatus.OVER) statusColor
                else MaterialTheme.colorScheme.onSurface
            )
        }

        if (!isTotal) {
            BudgetBar(
                fraction = row.fraction,
                color = statusColor,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * The track stays visible at every fill level, so a zero-spend category reads as intentional rather
 * than broken. Decorative only — the percentage is already in the row's text.
 */
@Composable
private fun BudgetBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

@Composable
private fun statusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.UNDER -> LocalChartColors.current.income
    BudgetStatus.NEAR -> LocalChartColors.current.warning
    BudgetStatus.OVER -> MaterialTheme.colorScheme.error
    BudgetStatus.UNBUDGETED -> MaterialTheme.colorScheme.tertiary
}

@PreviewLightDark
@Composable
private fun BudgetSummaryCardPreview() {
    // Wireframe figures, built through BudgetRow.of so the preview can't drift from real behaviour.
    val budgeted = listOf(
        BudgetRow.of("Groceries", 310.0, 400.0, unbudgeted = false),
        BudgetRow.of("Restaurant", 340.0, 200.0, unbudgeted = false),
        BudgetRow.of("Mobile", 0.0, 75.0, unbudgeted = false),
        BudgetRow.of("Going Out", 50.0, 250.0, unbudgeted = false),
    )
    val unbudgeted = listOf(
        BudgetRow.of("Clothing", 310.0, 400.0, unbudgeted = true),
        BudgetRow.of("Electronics", 10.0, 400.0, unbudgeted = true),
    )

    LedgerTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            BudgetSummaryCard(
                summary = BudgetSummary(
                    budgeted = budgeted,
                    budgetedTotal = BudgetRow.of(
                        "Total",
                        budgeted.sumOf { it.actual },
                        budgeted.sumOf { it.target },
                        unbudgeted = false,
                    ),
                    unbudgeted = unbudgeted,
                    unbudgetedTotal = BudgetRow.of(
                        "Total",
                        unbudgeted.sumOf { it.actual },
                        400.0,
                        unbudgeted = true,
                    ),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}
