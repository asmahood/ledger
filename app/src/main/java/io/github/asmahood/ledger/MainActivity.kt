package io.github.asmahood.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.asmahood.ledger.ui.navigation.FAB
import io.github.asmahood.ledger.ui.navigation.LedgerBottomBar
import io.github.asmahood.ledger.ui.navigation.LedgerNavHost
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerApp()
        }
    }
}

@Composable
fun LedgerApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    LedgerTheme {
        Scaffold(
            bottomBar = { LedgerBottomBar(navController = navController) },
            floatingActionButton = { FAB(onClick = {}) },
            modifier = modifier
        ) { contentPadding ->
            LedgerNavHost(navController = navController, modifier = Modifier.padding(contentPadding))
        }
    }
}

@PreviewLightDark
@Composable
private fun LedgerAppPreview() {
    LedgerApp()
}