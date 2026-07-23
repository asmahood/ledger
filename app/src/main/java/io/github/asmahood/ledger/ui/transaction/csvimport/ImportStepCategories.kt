package io.github.asmahood.ledger.ui.transaction.csvimport

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.csv.CategoryResolution
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.components.TransactionTypeSelector
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@Composable
fun ImportStepCategories(
    unmatchedCategories: List<String>,
    existingCategories: List<Category>,
    resolutions: Map<String, CategoryResolution>,
    onCategoryResolved: (String, CategoryResolution) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        item {
            Text(
                text = stringResource(R.string.import_category_unmatched),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(unmatchedCategories, key = { it }) { name ->
            CategoryResolutionCard(
                name = name,
                existingCategories = existingCategories,
                resolution = resolutions[name],
                onCategoryResolved = onCategoryResolved,
            )
        }
    }
}

/** The three ways one unmatched category name can be resolved. */
private enum class ResolutionMode(@StringRes val label: Int) {
    CREATE(R.string.import_category_create),
    EXISTING(R.string.import_category_existing),
    SKIP(R.string.import_category_skip);

    fun seed(existingCategories: List<Category>): CategoryResolution = when (this) {
        CREATE -> CategoryResolution.CreateNew(TransactionType.EXPENSE)
        EXISTING -> CategoryResolution.UseExisting(existingCategories.first().id)
        SKIP -> CategoryResolution.SkipRows
    }
}

private fun CategoryResolution?.mode(): ResolutionMode? = when (this) {
    is CategoryResolution.CreateNew -> ResolutionMode.CREATE
    is CategoryResolution.UseExisting -> ResolutionMode.EXISTING
    CategoryResolution.SkipRows -> ResolutionMode.SKIP
    null -> null
}

@Composable
private fun CategoryResolutionCard(
    name: String,
    existingCategories: List<Category>,
    resolution: CategoryResolution?,
    onCategoryResolved: (String, CategoryResolution) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            val selectedMode = resolution.mode()
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ResolutionMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ResolutionMode.entries.size,
                        ),
                        selected = mode == selectedMode,
                        // Nothing to point at when the app has no categories yet.
                        enabled = mode != ResolutionMode.EXISTING || existingCategories.isNotEmpty(),
                        onClick = { onCategoryResolved(name, mode.seed(existingCategories)) },
                        label = { Text(stringResource(mode.label)) },
                    )
                }
            }

            when (resolution) {
                is CategoryResolution.CreateNew -> TransactionTypeSelector(
                    selected = resolution.type,
                    onSelectedChange = { type ->
                        onCategoryResolved(name, CategoryResolution.CreateNew(type))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                is CategoryResolution.UseExisting -> ExistingCategoryDropdown(
                    categories = existingCategories,
                    selectedId = resolution.categoryId,
                    onSelected = { id ->
                        onCategoryResolved(name, CategoryResolution.UseExisting(id))
                    },
                )

                CategoryResolution.SkipRows, null -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExistingCategoryDropdown(
    categories: List<Category>,
    selectedId: Long,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        TextField(
            value = categories.find { it.id == selectedId }?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.import_category_existing)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelected(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ImportStepCategoriesPreview() {
    val groceries = Category(1, "Groceries", TransactionType.EXPENSE)
    val salary = Category(2, "Salary", TransactionType.INCOME)
    LedgerTheme {
        ImportStepCategories(
            unmatchedCategories = listOf("Coffee", "Rent", "Refunds"),
            existingCategories = listOf(groceries, salary),
            resolutions = mapOf(
                "Coffee" to CategoryResolution.CreateNew(TransactionType.EXPENSE),
                "Rent" to CategoryResolution.UseExisting(groceries.id),
                "Refunds" to CategoryResolution.SkipRows,
            ),
            onCategoryResolved = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}