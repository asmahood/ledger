package io.github.asmahood.ledger.ui.overview

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@Composable
fun OverviewScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = { LedgerTopBar(title = stringResource(R.string.overview)) },
        modifier = modifier,
    ) { contentPadding ->
        Text(text = "Overview screen!", modifier = Modifier.padding(contentPadding))
    }
}

@PreviewLightDark
@Composable
fun OverviewScreenPreviewL() {
    LedgerTheme {
        OverviewScreen()
    }
}