package io.github.asmahood.ledger.ui.manage.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme

@Composable
fun CategoryFormScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CategoryFormContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNameChange = viewModel::updateName,
        onDescriptionChange = viewModel::updateDescription,
        onTypeChange = viewModel::updateType,
        onSave = viewModel::saveCategory,
        onDelete = viewModel::deleteCategory,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    CategoryFormEvent.SavedSuccessfully -> onNavigateBack()
                    is CategoryFormEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
}

@Composable
fun CategoryFormContent(
    uiState: CategoryFormUiState,
    snackbarHostState: SnackbarHostState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LedgerTopBar(
                title = stringResource(if (uiState.id != 0L) R.string.edit_category else R.string.add_category),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.id != 0L) {
                        OutlinedIconButton(
                            onClick = { showDeleteDialog = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = stringResource(R.string.delete)
                            )
                        }
                    }

                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                TransactionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransactionType.entries.size
                        ),
                        onClick = { onTypeChange(type) },
                        selected = type == uiState.type,
                        label = { Text(text = stringResource(type.label)) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.name)) },
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.description)) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
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
                    onClick = onSave,
                    enabled = uiState.isFormValid,
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(text = stringResource(R.string.delete_category_title, uiState.name)) },
                text = { Text(text = stringResource(R.string.delete_category_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete()
                        }
                    ) {
                        Text(text = stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
fun CategoryFormContentComposable() {
    LedgerTheme {
        CategoryFormContent(
            uiState = CategoryFormUiState(
                name = "Groceries",
                type = TransactionType.EXPENSE,
                description = "Purchase from grocery stores"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNameChange = {},
            onDescriptionChange = {},
            onTypeChange = {},
            onSave = {},
            onNavigateBack = {},
            onDelete = {}
        )
    }
}

@PreviewLightDark
@Composable
fun CategoryFormContentEditComposable() {
    LedgerTheme {
        CategoryFormContent(
            uiState = CategoryFormUiState(
                id = 1,
                name = "Groceries",
                type = TransactionType.EXPENSE,
                description = "Purchase from grocery stores"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNameChange = {},
            onDescriptionChange = {},
            onTypeChange = {},
            onSave = {},
            onNavigateBack = {},
            onDelete = {}
        )
    }
}