package io.github.asmahood.ledger.ui.transaction.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionDayGroup
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.components.Avatar
import io.github.asmahood.ledger.ui.components.ErrorState
import io.github.asmahood.ledger.ui.components.LoadingState
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.util.formatCurrency
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun TransactionListScreen(
    onTransactionClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionListContent(
        uiState = uiState,
        onTransactionClicked = onTransactionClicked,
        modifier = modifier
    )
}

@Composable
private fun TransactionListContent(
    uiState: TransactionListUiState,
    onTransactionClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            LedgerTopBar(title = stringResource(R.string.transactions))
        }
    ) { contentPadding ->
        val contentModifier = modifier.padding(contentPadding)
        when (uiState) {
            is TransactionListUiState.Loading -> LoadingState(
                description = stringResource(R.string.transactions_loading_desc),
                modifier = contentModifier
            )

            is TransactionListUiState.Error -> ErrorState(
                title = stringResource(R.string.couldn_t_load_transactions),
                message = uiState.message,
                modifier = contentModifier
            )

            is TransactionListUiState.Success -> TransactionList(
                groups = uiState.groups,
                onTransactionClicked = onTransactionClicked,
                modifier = contentModifier
            )
        }
    }
}

private val fullDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

/** "Today" / "Yesterday" / "March 12, 2026" for a day group's [date]. */
@Composable
private fun dayHeaderLabel(date: LocalDate, today: LocalDate): String =
    when (val header = dayHeaderFor(date, today)) {
        DayHeader.Today -> stringResource(R.string.day_header_today)
        DayHeader.Yesterday -> stringResource(R.string.day_header_yesterday)
        is DayHeader.On -> header.date.format(fullDateFormatter)
    }

/** Expenses count negative, income positive, regardless of how the amount is stored. */
private fun Transaction.signedAmount(): Double =
    if (type == TransactionType.EXPENSE) -amount else amount

/** "+$1,500.00" / "-$42.18" — sign always shown, no space before the symbol (AC2). */
private fun formatSigned(value: Double): String {
    val sign = if (value >= 0) "+" else "-"
    return "$sign${formatCurrency(abs(value))}"
}

/**
 * Transactions grouped by day: a serif date header over a card holding that day's rows, divided
 * by hairline rules.
 */
@Composable
private fun TransactionList(
    groups: List<TransactionDayGroup>,
    onTransactionClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        items(groups, key = { it.date.toEpochDay() }) { group ->
            Column {
                Text(
                    text = dayHeaderLabel(group.date, today),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        group.transactions.forEachIndexed { index, transaction ->
                            TransactionRow(transaction, onTransactionClicked)
                            if (index < group.transactions.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(start = 72.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One transaction row: avatar · vendor (bold) · category (italic) · signed amount. */
@Composable
private fun TransactionRow(
    transaction: Transaction,
    onTransactionClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(text = transaction.vendor, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Text(text = transaction.category.name, fontStyle = FontStyle.Italic)
        },
        leadingContent = { Avatar(transaction.category) },
        trailingContent = { SignedAmount(transaction) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.clickable { onTransactionClicked(transaction.id) },
    )
}

/** Signed amount as plain text, colored green for income and red for expense (AC2). */
@Composable
private fun SignedAmount(transaction: Transaction, modifier: Modifier = Modifier) {
    val value = transaction.signedAmount()
    val color =
        if (value >= 0) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error
    Text(
        text = formatSigned(value),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun TransactionListPreview() {
    val groceries = Category(0, "Groceries", TransactionType.EXPENSE)
    val transport = Category(1, "Transport", TransactionType.EXPENSE)
    val salary = Category(2, "Salary", TransactionType.INCOME)
    val utilities = Category(3, "Utilities", TransactionType.EXPENSE)
    val dining = Category(4, "Dining", TransactionType.EXPENSE)
    val coffee = Category(5, "Coffee", TransactionType.EXPENSE)
    val today = LocalDate.now()
    LedgerTheme {
        TransactionListContent(
            uiState = TransactionListUiState.Success(
                listOf(
                    TransactionDayGroup(
                        date = today,
                        transactions = listOf(
                            Transaction(0, 42.18, today, "Food Basics", TransactionType.EXPENSE, null, groceries),
                            Transaction(1, 18.40, today, "Rideshare", TransactionType.EXPENSE, null, transport),
                            Transaction(2, 5.75, today, "Blue Bottle", TransactionType.EXPENSE, null, coffee),
                        )
                    ),
                    TransactionDayGroup(
                        date = today.minusDays(1),
                        transactions = listOf(
                            Transaction(3, 1500.00, today.minusDays(1), "Employer", TransactionType.INCOME, null, salary),
                            Transaction(4, 96.25, today.minusDays(1), "Hydro One", TransactionType.EXPENSE, null, utilities),
                        )
                    ),
                    TransactionDayGroup(
                        date = today.minusDays(3),
                        transactions = listOf(
                            Transaction(5, 88.20, today.minusDays(3), "The Keg", TransactionType.EXPENSE, null, dining),
                        )
                    ),
                )
            ),
            onTransactionClicked = {}
        )
    }
}
