package io.github.asmahood.ledger.ui.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.Transaction
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.util.formatCurrency

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
        val contentPadding = modifier.padding(contentPadding)
        when (uiState) {
            is TransactionListUiState.Loading -> LoadingState(contentPadding)
            is TransactionListUiState.Error -> ErrorState(
                message = uiState.message,
                modifier = contentPadding
            )

            is TransactionListUiState.Success -> TransactionList(uiState.transactions, contentPadding)
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    val loadingDesc = stringResource(R.string.manage_loading_desc)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = loadingDesc },
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.couldn_t_load_transactions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
fun TransactionCard(transaction: Transaction, modifier: Modifier = Modifier) {
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

@Composable
private fun Avatar(category: Category) {
    val container = when (category.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.tertiaryContainer
        TransactionType.INCOME -> MaterialTheme.colorScheme.primaryContainer
    }
    val onContainer = when (category.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onTertiaryContainer
        TransactionType.INCOME -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(container, RoundedCornerShape(50)),
    ) {
        Text(
            text = category.name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = onContainer,
        )
    }
}

@Composable
private fun AmountPill(amount: Double, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@PreviewLightDark
@Composable
fun TransactionListPreview() {
    LedgerTheme {

    }
}
