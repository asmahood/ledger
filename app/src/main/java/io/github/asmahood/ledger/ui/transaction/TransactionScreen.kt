package io.github.asmahood.ledger.ui.transaction

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@Composable
fun TransactionScreen(modifier: Modifier = Modifier) {
    Text(text = "Transaction Screen", modifier = modifier)
}

@PreviewLightDark
@Composable
fun TransactionScreenPreview() {
    LedgerTheme {
        TransactionScreen()
    }
}