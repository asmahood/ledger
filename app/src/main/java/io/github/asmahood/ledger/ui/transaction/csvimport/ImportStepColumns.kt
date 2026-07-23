package io.github.asmahood.ledger.ui.transaction.csvimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.csv.ColumnMapping
import io.github.asmahood.ledger.data.csv.ImportField
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@Composable
fun ImportStepColumns(
    headers: List<String>,
    mapping: ColumnMapping,
    onColumnMapped: (ImportField, Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(ImportField.entries, key = { it.name }) { field ->
            ColumnDropdown(
                field = field,
                headers = headers,
                selectedIndex = mapping[field],
                onSelected = { index -> onColumnMapped(field, index) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnDropdown(
    field: ImportField,
    headers: List<String>,
    selectedIndex: Int?,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val ignoreLabel = stringResource(R.string.import_column_ignore)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        TextField(
            value = selectedIndex?.let { headers.getOrNull(it) } ?: ignoreLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(field.label)) },
            supportingText = if (field.required) {
                { Text(stringResource(R.string.import_column_required)) }
            } else {
                null
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(ignoreLabel) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            headers.forEachIndexed { index, header ->
                DropdownMenuItem(
                    text = { Text(header) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ImportStepColumnsPreview() {
    val headers = listOf("Date", "Amount", "Category", "Vendor", "Type", "Note")
    LedgerTheme {
        ImportStepColumns(
            headers = headers,
            mapping = ColumnMapping.autoDetect(headers),
            onColumnMapped = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}