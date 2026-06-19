package io.github.asmahood.ledger.ui.manage.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@Composable
fun CategoryFormScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(stringResource(R.string.expense), stringResource(R.string.income))

    var selectedType by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            LedgerTopBar(
                title = stringResource(R.string.add_category),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack)  {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
            )
        },
        modifier = modifier
    ) { contentPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth()

        ) {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        onClick = { selectedType = index },
                        selected = index == selectedType,
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) }
            )

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { /* TODO: save via viewModel, then navigate back */ },
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }

    }
}

@PreviewLightDark
@Composable
fun CategoryFormScreenComposable() {
    LedgerTheme {
        CategoryFormScreen(onNavigateBack = {})
    }
}