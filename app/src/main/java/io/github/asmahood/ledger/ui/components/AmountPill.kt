package io.github.asmahood.ledger.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.util.formatCurrency

/**
 * Rounded pill showing a formatted currency [amount]. Pass [suffix] to append a unit such as
 * "/mo" for recurring budgets.
 */
@Composable
fun AmountPill(amount: Double?, modifier: Modifier = Modifier, suffix: String = "") {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = if (amount != null) "${formatCurrency(amount)}$suffix" else "-",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun AmountPillPreview() {
    LedgerTheme {
        Surface {
            AmountPill(150.00, suffix = "/mo")
        }
    }
}
