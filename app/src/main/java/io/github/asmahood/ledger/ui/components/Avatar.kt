package io.github.asmahood.ledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.theme.LedgerTheme

/** Circular monogram showing a category's first initial, tinted by its [TransactionType]. */
@Composable
fun Avatar(category: Category, modifier: Modifier = Modifier) {
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
        modifier = modifier
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

@PreviewLightDark
@Composable
private fun AvatarPreview() {
    LedgerTheme {
        Surface {
            Avatar(Category(0, "Gas", TransactionType.EXPENSE))
        }
    }
}
