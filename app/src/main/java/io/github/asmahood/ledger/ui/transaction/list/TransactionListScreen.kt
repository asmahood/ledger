package io.github.asmahood.ledger.ui.transaction.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.components.AmountPill
import io.github.asmahood.ledger.ui.components.Avatar
import io.github.asmahood.ledger.ui.components.ErrorState
import io.github.asmahood.ledger.ui.components.LoadingState
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import java.time.LocalDate

@Composable
fun TransactionListScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionListContent(
        uiState = uiState,
        modifier = modifier
    )
}

@Composable
private fun TransactionListContent(
    uiState: TransactionListUiState,
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

            is TransactionListUiState.Success -> TransactionList(uiState.transactions, contentModifier)
        }
    }
}

@Composable
private fun TransactionList(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(transactions, key = { it.id }) { transaction ->
            TransactionCard(
                transaction = transaction
            )
        }
    }
}

@Composable
private fun TransactionCard(transaction: Transaction, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(size = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(text = transaction.vendor, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Text(text = transaction.category.name, fontStyle = FontStyle.Italic)
            },
            leadingContent = { Avatar(transaction.category) },
            trailingContent = { AmountPill(transaction.amount) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@PreviewLightDark
@Composable
private fun TransactionListPreview() {
    val groceries = Category(0, "Groceries", TransactionType.EXPENSE)
    val salary = Category(1, "Salary", TransactionType.INCOME)
    LedgerTheme {
        TransactionListContent(
            uiState = TransactionListUiState.Success(
                listOf(
                    Transaction(0, 42.18, LocalDate.now(), "Food Basics", TransactionType.EXPENSE, null, groceries),
                    Transaction(1, 1500.00, LocalDate.now(), "Employer", TransactionType.INCOME, null, salary),
                )
            )
        )
    }
}
