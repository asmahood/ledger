package io.github.asmahood.ledger.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.ui.components.ErrorState
import io.github.asmahood.ledger.ui.components.LoadingState
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.ui.theme.figureFontFamily
import io.github.asmahood.ledger.util.formatCurrency

@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()

    OverviewContent(
        uiState = uiState,
        selectedPeriod = selectedPeriod,
        onPeriodSelected = viewModel::onPeriodSelected,
        modifier = modifier
    )
}

@Composable
internal fun OverviewContent(
    uiState: OverviewUiState,
    selectedPeriod: OverviewPeriod,
    onPeriodSelected: (OverviewPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = { LedgerTopBar(title = stringResource(R.string.overview)) },
        modifier = modifier,
    ) { contentPadding ->
        val contentModifier = modifier.padding(contentPadding)
        when (uiState) {
            is OverviewUiState.Loading -> LoadingState(
                description = stringResource(R.string.loading_summary),
                modifier = contentModifier
            )

            is OverviewUiState.Error -> ErrorState(
                title = stringResource(R.string.couldnt_load_summary),
                message = uiState.message,
                modifier = contentModifier
            )

            is OverviewUiState.Success -> LazyColumn(
                modifier = Modifier.padding(contentPadding),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(OverviewPeriod.entries) { period ->
                            FilterChip(
                                label = { Text(stringResource(period.label)) },
                                selected = selectedPeriod == period,
                                onClick = { onPeriodSelected(period) }
                            )
                        }
                    }
                }

                item {
                    SummaryCard(
                        summary = uiState.summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }

    }
}


@PreviewLightDark
@Composable
private fun OverviewScreenPreview() {
    LedgerTheme {
        OverviewContent(
            uiState = OverviewUiState.Success(
                summary = OverviewSummary(
                    income = 4200.0,
                    expenses = 3300.0,
                    saved = 1100.0,
                    expensesPercentOfIncome = 74,
                    savedPercentOfIncome = 26
                )
            ),
            selectedPeriod = OverviewPeriod.THIS_MONTH,
            onPeriodSelected = {}
        )
    }
}

@Composable
private fun SummaryCard(
    summary: OverviewSummary,
    modifier: Modifier = Modifier,
) {
    val savedColor =
        if (summary.isNegativeSaved) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    SummaryEyebrow(stringResource(R.string.income))
                    Text(
                        text = formatCurrency(summary.income),
                        style = figureStyle(MaterialTheme.typography.titleMedium)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    SummaryEyebrow(stringResource(R.string.expenses))
                    Text(
                        text = formatCurrency(summary.expenses),
                        style = figureStyle(MaterialTheme.typography.titleMedium)
                    )
                    summary.expensesPercentOfIncome?.let { percent ->
                        Text(
                            text = stringResource(R.string.percent_of_income, percent),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SummaryEyebrow(stringResource(R.string.saved))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatCurrency(summary.saved),
                    style = figureStyle(MaterialTheme.typography.displaySmall),
                    fontWeight = FontWeight.SemiBold,
                    color = savedColor
                )
                summary.savedPercentOfIncome?.let { percent ->
                    Text(
                        text = stringResource(R.string.percent_of_income, percent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp,
        modifier = modifier
    )
}

@Composable
private fun figureStyle(style: TextStyle) = style.copy(fontFamily = figureFontFamily)

@PreviewLightDark
@Composable
private fun SummaryCardPreview() {
    LedgerTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            SummaryCard(
                summary = OverviewSummary(
                    income = 4200.0,
                    expenses = 3100.0,
                    saved = 1100.0,
                    expensesPercentOfIncome = 74,
                    savedPercentOfIncome = 26
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SummaryCardNegativePreview() {
    LedgerTheme {
        Surface(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            SummaryCard(
                summary = OverviewSummary(
                    income = 3100.0,
                    expenses = 4200.0,
                    saved = -1100.0,
                    expensesPercentOfIncome = 135,
                    savedPercentOfIncome = -35
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}