package io.github.asmahood.ledger.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.asmahood.ledger.data.model.TransactionType

/** Segmented control for choosing a [TransactionType] (Expense / Income). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTypeSelector(
    selected: TransactionType,
    onSelectedChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        TransactionType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TransactionType.entries.size
                ),
                onClick = { onSelectedChange(type) },
                selected = type == selected,
                label = { Text(text = stringResource(type.label)) }
            )
        }
    }
}
