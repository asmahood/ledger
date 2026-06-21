package io.github.asmahood.ledger.ui.manage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.asmahood.ledger.R
import io.github.asmahood.ledger.data.model.Category
import io.github.asmahood.ledger.data.model.TransactionType
import io.github.asmahood.ledger.ui.navigation.LedgerTopBar
import io.github.asmahood.ledger.ui.theme.LedgerTheme
import io.github.asmahood.ledger.util.formatCurrency

@Composable
fun ManageScreenContent(
    onAddCategoryClicked: () -> Unit,
    onCategoryClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ManageScreenContent(
        onAddCategoryClicked = onAddCategoryClicked,
        onCategoryClicked = onCategoryClicked,
        uiState = uiState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreenContent(
    uiState: ManageUiState,
    onAddCategoryClicked: () -> Unit,
    onCategoryClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            LedgerTopBar(
                title = stringResource(R.string.manage_app_bar_title),
                actions = {
                    FilledIconButton(onClick = onAddCategoryClicked) {
                        Icon(
                            painter = painterResource(R.drawable.add),
                            contentDescription = stringResource(R.string.add_category)
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { contentPadding ->
        val contentModifier = Modifier.padding(contentPadding)
        when (uiState) {
            is ManageUiState.Loading -> LoadingState(contentModifier)
            is ManageUiState.Error -> ErrorState(
                message = uiState.message,
                modifier = contentModifier
            )

            is ManageUiState.Success -> {
                CategoryList(
                    uiState.expenseCategories,
                    uiState.incomeCategories,
                    onCategoryClicked = onCategoryClicked,
                    contentModifier
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    val loadingDesc = stringResource(R.string.manage_loading_desc)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = loadingDesc },
    ) {
        CircularProgressIndicator()
    }
}

@PreviewLightDark
@Composable
private fun LoadingStatePreview() {
    LedgerTheme {
        Surface { LoadingState() }
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.manage_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewLightDark
@Composable
private fun ErrorStatePreview() {
    LedgerTheme {
        Surface { ErrorState(message = "Database unavailable") }
    }
}

@Composable
private fun CategoryList(
    expenseCategories: List<Category>,
    incomeCategories: List<Category>,
    onCategoryClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expensesOpen by rememberSaveable { mutableStateOf(true) }
    var incomeOpen by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        item {
            SectionHeader(
                title = stringResource(R.string.expenses),
                expanded = expensesOpen,
                count = expenseCategories.size,
                onToggle = { expensesOpen = !expensesOpen })
        }
        if (expensesOpen) {
            items(expenseCategories, key = { it.id }) { cat ->
                CategoryCard(
                    category = cat,
                    budget = 150.00,
                    onCategoryClicked = onCategoryClicked
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            SectionHeader(
                title = stringResource(R.string.income),
                expanded = incomeOpen,
                count = incomeCategories.size,
                onToggle = { incomeOpen = !incomeOpen })
        }
        if (incomeOpen) {
            items(incomeCategories, key = { it.id }) { cat ->
                CategoryCard(
                    category = cat,
                    budget = 150.00,
                    onCategoryClicked = onCategoryClicked
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun CategoryListPreview() {
    val testExpenseCategories = listOf<Category>(
        Category(0, "Entertainment", TransactionType.EXPENSE, "Hobbies, concerts, shows"),
        Category(1, "Gas", TransactionType.EXPENSE, null),
        Category(2, "Going Out", TransactionType.EXPENSE, "Bars, outings with friends"),
        Category(3, "Groceries", type = TransactionType.EXPENSE, null),
        Category(4, "Gym", TransactionType.EXPENSE, null)
    )

    val testIncomeCategories = listOf<Category>(
        Category(5, "Job", TransactionType.INCOME, null),
        Category(6, "Tax Refund", TransactionType.INCOME, null),
        Category(7, "Side Project", TransactionType.INCOME, "Freelance work, side income"),
        Category(8, "Expense Reimbursement", TransactionType.INCOME, null),
    )

    LedgerTheme {
        Surface {
            CategoryList(testExpenseCategories, testIncomeCategories, {})
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    budget: Double,
    onCategoryClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(size = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onCategoryClicked(category.id) },
        modifier = modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(text = category.name, fontWeight = FontWeight.SemiBold)
            },
            supportingContent = if (category.description != null) {
                { Text(text = category.description, fontStyle = FontStyle.Italic) }
            } else null,
            leadingContent = { Avatar(category) },
            trailingContent = { AmountPill(budget) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@PreviewLightDark
@Composable
private fun CategoryCardPreview() {
    LedgerTheme {
        Surface {
            CategoryCard(
                Category(0, "Going out", TransactionType.EXPENSE, "Bars, dates, adventures"),
                150.00,
                {}
            )
        }
    }
}

@Composable
private fun AmountPill(budget: Double, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Text(
            text = "${formatCurrency(budget)}/mo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@PreviewLightDark
@Composable
fun AmountPillPreview() {
    LedgerTheme {
        Surface {
            AmountPill(150.00)
        }
    }
}

@Composable
private fun Avatar(category: Category) {
    val container = when (category.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.tertiaryContainer
        TransactionType.INCOME -> MaterialTheme.colorScheme.primaryContainer
    }
    val onContainer = when (category.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onTertiaryContainer
        TransactionType.INCOME -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(container, RoundedCornerShape(50)),
    ) {
        Text(
            text = category.name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = onContainer,
        )
    }
}

@PreviewLightDark
@Composable
fun AvatarPreview() {
    LedgerTheme {
        Surface {
            Avatar(Category(0, "Gas", TransactionType.EXPENSE))
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    count: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (expanded) 0f else -90f, label = "chevron")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(8.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.expand_more),
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotation),
        )
    }
}

@PreviewLightDark
@Composable
fun SectionHeaderPreview() {
    LedgerTheme {
        Surface {
            SectionHeader(title = "Income", expanded = false, count = 5, onToggle = {})
        }
    }
}