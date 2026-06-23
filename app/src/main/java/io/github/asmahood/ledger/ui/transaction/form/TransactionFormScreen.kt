package io.github.asmahood.ledger.ui.transaction.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.components.TransactionTypeSelector
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.util.transactionDateFormatter
import java.time.Instant
import java.time.ZoneId

@Composable
fun TransactionFormScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    TransactionFormContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTypeChange = viewModel::updateType,
        onAmountChange = viewModel::updateAmount,
        onCategoryChange = viewModel::updateCategory,
        onDateChange = viewModel::updateDate,
        onVendorChange = viewModel::updateVendor,
        onNotesChange = viewModel::updateNotes,
        onNavigateBack = onNavigateBack,
        onSave = viewModel::saveTransaction,
        onDelete = viewModel::deleteTransaction,
        modifier = modifier
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    TransactionFormEvent.SavedSuccessfully,
                    TransactionFormEvent.Dismissed -> onNavigateBack()
                    is TransactionFormEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormContent(
    uiState: TransactionFormUiState,
    snackbarHostState: SnackbarHostState,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onDateChange: (String) -> Unit,
    onVendorChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoriesExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            onDateChange(
                Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate()
                    .format(transactionDateFormatter)
            )
            showDatePicker = false
        }
    }

    Scaffold(
        topBar = {
            LedgerTopBar(
                title = stringResource(if (uiState.isEditMode) R.string.edit_transaction else R.string.add_transaction),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        OutlinedIconButton(onClick = { showDeleteDialog = true }) {
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
            TransactionTypeSelector(
                selected = uiState.type,
                onSelectedChange = onTypeChange
            )

            OutlinedTextField(
                value = uiState.amount,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.amount)) },
                prefix = { Text(stringResource(R.string.dollar_sign)) },
                placeholder = { Text(stringResource(R.string.amount_placeholder)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )

            ExposedDropdownMenuBox(
                expanded = categoriesExpanded,
                onExpandedChange = { categoriesExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.category?.name ?: stringResource(R.string.pick_a_category),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = categoriesExpanded,
                    onDismissRequest = { categoriesExpanded = false }
                ) {
                    uiState.visibleCategories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                onCategoryChange(option)
                                categoriesExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Box {
                OutlinedTextField(
                    value = uiState.date,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.date)) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = !showDatePicker }) {
                            Icon(
                                painter = painterResource(R.drawable.date_range),
                                contentDescription = stringResource(R.string.select_date)
                            )
                        }
                    },
                    supportingText = { Text(stringResource(R.string.date_supporting_text)) }
                )
                if (showDatePicker) {
                    Popup(
                        onDismissRequest = { showDatePicker = false },
                        alignment = Alignment.TopStart,
                        properties = PopupProperties(focusable = true),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 64.dp)
                                .shadow(elevation = 4.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            DatePicker(
                                state = datePickerState,
                                showModeToggle = false,
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.vendor,
                onValueChange = onVendorChange,
                label = {
                    Text(
                        if (uiState.type == TransactionType.EXPENSE) stringResource(R.string.store_vendor) else stringResource(
                            R.string.source_payer
                        )
                    )
                },
                placeholder = { Text(stringResource(R.string.vendor_placeholder)) },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.notes)) },
                supportingText = { Text(stringResource(R.string.notes_supporting_text)) }
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
                title = { Text(text = stringResource(R.string.delete_this_transaction)) },
                text = { Text(text = stringResource(R.string.delete_transaction_message)) },
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
fun TransactionFormContentCreatePreview() {
    LedgerTheme {
        TransactionFormContent(
            uiState = TransactionFormUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onTypeChange = {},
            onAmountChange = {},
            onCategoryChange = {},
            onDateChange = {},
            onVendorChange = {},
            onNotesChange = {},
            onNavigateBack = {},
            onSave = {},
            onDelete = {}
        )
    }
}