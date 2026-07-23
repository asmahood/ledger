package io.github.asmahood.ledger.ui.transaction.csvimport

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.csv.CategoryResolution
import io.github.asmahood.ledger.data.csv.ColumnMapping
import io.github.asmahood.ledger.data.csv.ImportField
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme

/** The four steps the indicator counts; RESULT is a terminal screen, not a step. */
private val WizardSteps = listOf(
    ImportStep.FILE, ImportStep.COLUMNS, ImportStep.CATEGORIES, ImportStep.PREVIEW,
)

@Composable
fun ImportScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ImportScreenContent(
        uiState = uiState,
        onFileSelected = viewModel::onFileSelected,
        onColumnMapped = viewModel::onColumnMapped,
        onCategoryResolved = viewModel::onCategoryResolved,
        onRowToggled = viewModel::onRowToggled,
        onNext = viewModel::onNext,
        onBack = viewModel::onBack,
        onImport = viewModel::onImport,
        onClose = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
fun ImportScreenContent(
    uiState: ImportUiState,
    onFileSelected: (Uri) -> Unit,
    onColumnMapped: (ImportField, Int?) -> Unit,
    onCategoryResolved: (String, CategoryResolution) -> Unit,
    onRowToggled: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onImport: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            LedgerTopBar(
                title = stringResource(R.string.import_transactions),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            ImportBottomBar(
                uiState = uiState,
                onBack = onBack,
                onNext = onNext,
                onImport = onImport,
                onClose = onClose,
            )
        },
        modifier = modifier,
    ) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            if (uiState.step != ImportStep.RESULT) {
                StepIndicator(step = uiState.step)
            }
            if (uiState.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            when (uiState.step) {
                ImportStep.FILE -> ImportStepFile(
                    fileName = uiState.fileName,
                    rowCount = uiState.rawRows.size,
                    onFileSelected = onFileSelected,
                )

                ImportStep.COLUMNS -> ImportStepColumns(
                    headers = uiState.headers,
                    mapping = uiState.mapping,
                    onColumnMapped = onColumnMapped,
                )

                ImportStep.CATEGORIES -> ImportStepCategories(
                    unmatchedCategories = uiState.unmatchedCategories,
                    existingCategories = uiState.existingCategories,
                    resolutions = uiState.resolutions,
                    onCategoryResolved = onCategoryResolved,
                )

                ImportStep.PREVIEW -> ImportStepPreview(
                    rows = uiState.rows,
                    duplicateLines = uiState.duplicateLines,
                    selectedLines = uiState.selectedLines,
                    onRowToggled = onRowToggled,
                )

                ImportStep.RESULT -> uiState.result?.let { ImportResultSummary(result = it) }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: ImportStep, modifier: Modifier = Modifier) {
    val index = WizardSteps.indexOf(step).coerceAtLeast(0)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WizardSteps.forEachIndexed { position, _ ->
                Box(
                    modifier = Modifier
                        .size(if (position == index) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (position <= index) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${stringResource(R.string.import_step_of, index + 1, WizardSteps.size)} — " +
                    stringResource(stepTitle(step)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@StringRes
private fun stepTitle(step: ImportStep): Int = when (step) {
    ImportStep.FILE -> R.string.import_step_file
    ImportStep.COLUMNS -> R.string.import_step_columns
    ImportStep.CATEGORIES -> R.string.import_step_categories
    ImportStep.PREVIEW -> R.string.import_step_preview
    ImportStep.RESULT -> R.string.import_result_title
}

@Composable
private fun ImportBottomBar(
    uiState: ImportUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onImport: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(tonalElevation = 2.dp, modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            if (uiState.step != ImportStep.FILE && uiState.step != ImportStep.RESULT) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.import_action_back))
                }
            }
            when (uiState.step) {
                ImportStep.PREVIEW -> Button(
                    onClick = onImport,
                    enabled = !uiState.loading && uiState.selectedCount > 0,
                ) {
                    Text(stringResource(R.string.import_action_import, uiState.selectedCount))
                }

                ImportStep.RESULT -> Button(onClick = onClose) {
                    Text(stringResource(R.string.import_action_done))
                }

                else -> Button(onClick = onNext, enabled = uiState.canGoNext) {
                    Text(stringResource(R.string.import_action_next))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ImportScreenPreview() {
    LedgerTheme {
        ImportScreenContent(
            uiState = ImportUiState(
                step = ImportStep.COLUMNS,
                fileName = "budget-2026.csv",
                headers = listOf("Date", "Amount", "Category", "Vendor", "Type", "Note"),
                mapping = ColumnMapping.autoDetect(
                    listOf("Date", "Amount", "Category", "Vendor", "Type", "Note")
                ),
                rawRows = listOf(listOf("2026-01-05", "12.50", "Groceries", "Food Basics", "Expense", "")),
            ),
            onFileSelected = {},
            onColumnMapped = { _, _ -> },
            onCategoryResolved = { _, _ -> },
            onRowToggled = {},
            onNext = {},
            onBack = {},
            onImport = {},
            onClose = {},
        )
    }
}