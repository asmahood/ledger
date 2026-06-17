package io.github.asmahood.ledger.ui.manage

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@Composable
fun ManageScreen(modifier: Modifier = Modifier) {
    Text(text = "Manage Screen", modifier = modifier)
}

@PreviewLightDark
@Composable
fun ManageScreenPreview() {
    LedgerTheme {
        ManageScreen()
    }
}