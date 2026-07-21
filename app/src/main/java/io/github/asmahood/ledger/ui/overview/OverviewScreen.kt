package io.github.asmahood.ledger.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.LineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.ui.components.ErrorState
import io.github.asmahood.ledger.ui.components.LoadingState
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.ui.theme.figureStyle
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
        onCategoryToggled = viewModel::onCategoryToggled,
        modifier = modifier
    )
}

@Composable
internal fun OverviewContent(
    uiState: OverviewUiState,
    selectedPeriod: OverviewPeriod,
    onPeriodSelected: (OverviewPeriod) -> Unit,
    onCategoryToggled: (Long) -> Unit,
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
                modifier = contentModifier,
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

                item {
                    TotalIncomeChartCard(
                        chart = uiState.totalIncomeChart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                item {
                    CategorySpendChartCard(
                        chart = uiState.categorySpendChart,
                        onCategoryToggled = onCategoryToggled,
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
                ),
                categorySpendChart = CategorySpendChart(
                    monthLabels = listOf("Jan '26", "Feb '26", "Mar '26"),
                    series = listOf(
                        CategorySeries(
                            categoryId = 1,
                            categoryName = "Gas",
                            amounts = listOf(56.0, 45.85, 73.22)
                        ),
                        CategorySeries(
                            categoryId = 2,
                            categoryName = "Rent",
                            amounts = listOf(1500.0, 1500.0, 1500.0)
                        )
                    )
                ),
                totalIncomeChart = TotalIncomeChart(
                    monthLabels = listOf("Jan '26", "Feb '26", "Mar '26"),
                    amounts = listOf(4200.0, 3950.0, 4600.0)
                )
            ),
            selectedPeriod = OverviewPeriod.THIS_MONTH,
            onPeriodSelected = {},
            onCategoryToggled = {}
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

/**
 * Shared column-chart host for the overview's monthly charts: a currency start axis, a month-label
 * bottom axis, and an optional tap marker. Callers supply the model producer, columns, and (for
 * multi-series charts) the merge mode.
 */
@Composable
private fun MonthlyColumnChartHost(
    modelProducer: CartesianChartModelProducer,
    columnProvider: ColumnCartesianLayer.ColumnProvider,
    monthLabels: List<String>,
    modifier: Modifier = Modifier,
    mergeMode: (ExtraStore) -> ColumnCartesianLayer.MergeMode = {
        ColumnCartesianLayer.MergeMode.Grouped()
    },
    marker: CartesianMarker? = null
) {
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = columnProvider,
                mergeMode = mergeMode
            ),
            startAxis = VerticalAxis.rememberStart(
                itemPlacer = remember { VerticalAxis.ItemPlacer.count(count = { 5 }) },
                valueFormatter = remember {
                    CartesianValueFormatter { _, value, _ -> formatCurrency(value) }
                }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = remember(monthLabels) {
                    CartesianValueFormatter { _, x, _ ->
                        monthLabels.getOrNull(x.toInt())
                            ?: monthLabels.lastOrNull()
                            ?: x.toInt().toString()
                    }
                }
            ),
            marker = marker
        ),
        modelProducer = modelProducer,
        modifier = modifier
    )
}

@Composable
private fun CategorySpendChartCard(
    chart: CategorySpendChart,
    onCategoryToggled: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val visibleSeries = chart.series.filter { it.isVisible }
    val visibleColors = visibleSeries.map { categoryColor(it.categoryId) }
    val columnProvider = remember(visibleColors) {
        ColumnCartesianLayer.ColumnProvider.series(
            visibleColors.ifEmpty { listOf(Color.Transparent) }.map { color ->
                LineComponent(Fill(color), 16.dp)
            }
        )
    }

    LaunchedEffect(chart) {
        if (chart.monthLabels.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnModel {
                if (visibleSeries.isEmpty()) {
                    series(y = List(chart.monthLabels.size) { 0.0 })
                } else {
                    visibleSeries.forEach { series ->
                        series(y = series.amounts.map { it.toFloat() })
                    }
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SummaryEyebrow("Expenses by category")

                if (chart.series.isNotEmpty()) {
                    var menuExpanded by remember { mutableStateOf(false) }

                    Box {
                        TextButton(onClick = { menuExpanded = true }) {
                            Text("Categories")
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            chart.series.forEach { series ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = series.isVisible,
                                                onCheckedChange = null
                                            )
                                            ColorSwatch(
                                                color = categoryColor(series.categoryId),
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                            Text(
                                                text = series.categoryName,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    },
                                    onClick = { onCategoryToggled(series.categoryId) }
                                )
                            }
                        }
                    }
                }
            }

            MonthlyColumnChartHost(
                modelProducer = modelProducer,
                columnProvider = columnProvider,
                monthLabels = chart.monthLabels,
                mergeMode = { ColumnCartesianLayer.MergeMode.Stacked },
                modifier = Modifier.padding(top = 12.dp)
            )

            if (visibleSeries.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    visibleSeries.forEach { series ->
                        LegendItem(
                            name = series.categoryName,
                            color = categoryColor(series.categoryId)
                        )
                    }
                }
            }
        }
    }
}

/** Identifies the total-income chart card in UI tests, independent of any user-visible label. */
internal const val TotalIncomeChartTestTag = "total_income_chart_card"

@Composable
private fun TotalIncomeChartCard(
    chart: TotalIncomeChart,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val columnColor = MaterialTheme.colorScheme.primary
    val columnProvider = remember(columnColor) {
        ColumnCartesianLayer.ColumnProvider.series(
            listOf(LineComponent(Fill(columnColor), 16.dp))
        )
    }

    // AC2: tapping a bar reveals that month's exact income. The marker shows on press by default.
    // Format from the source amounts (Double) keyed by the tapped x rather than the model's
    // Float-widened y, so the label is exact even for amounts beyond Float's precision.
    val incomeMarker = rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
            padding = Insets(horizontal = 8.dp, vertical = 4.dp),
            background = rememberShapeComponent(
                fill = Fill(MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(8.dp)
            )
        ),
        valueFormatter = remember(chart.amounts) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val index = (targets.firstOrNull() as? ColumnCartesianLayerMarkerTarget)?.x?.toInt()
                index?.let { chart.amounts.getOrNull(it) }?.let(::formatCurrency) ?: ""
            }
        }
    )

    LaunchedEffect(chart) {
        if (chart.monthLabels.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            columnModel {
                series(y = chart.amounts.map { it.toFloat() })
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag(TotalIncomeChartTestTag)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SummaryEyebrow("Income over time")

            MonthlyColumnChartHost(
                modelProducer = modelProducer,
                columnProvider = columnProvider,
                monthLabels = chart.monthLabels,
                marker = incomeMarker,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/**
 * Derives a stable color from the category's identity rather than its position, so a category
 * keeps the same color across periods and when other categories are added or removed. The golden
 * angle (137.5°) spreads sequential ids across the hue wheel.
 */
private fun categoryColor(categoryId: Long): Color {
    val hue = (categoryId * 137.5f) % 360f
    return Color.hsl(hue, 0.55f, 0.50f)
}

@Composable
private fun ColorSwatch(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .background(color, RoundedCornerShape(2.dp))
    )
}

@Composable
private fun LegendItem(name: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        ColorSwatch(color = color)
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
