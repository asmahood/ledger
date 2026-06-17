package io.github.asmahood.ledger.ui.overview

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@Composable
fun OverviewScreen(modifier: Modifier = Modifier) {
    Text(text = "Overview screen!", modifier = modifier)
}

@PreviewLightDark
@Composable
fun OverviewScreenPreviewL() {
    LedgerTheme {
        OverviewScreen()
    }
}