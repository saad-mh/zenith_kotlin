package com.saadm.zenith.ui.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.room.withTransaction
import com.saadm.zenith.data.db.DatabaseProvider
import com.saadm.zenith.data.db.dao.CategoryDao
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.entity.PayeeEntity
import com.saadm.zenith.data.entity.TransactionEntity
import com.saadm.zenith.data.entity.TxnType
import com.saadm.zenith.ui.components.CategoryPicker
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.coroutines.launch

private enum class AddStep {
    Type,
    Payee,
    Details
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionFlow(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appDatabase = remember(context.applicationContext) {
        DatabaseProvider.getInstance(context.applicationContext)
    }
    val transactionDao = remember(appDatabase) { appDatabase.transactionDao() }
    val payeeDao = remember(appDatabase) { appDatabase.payeeDao() }
    val categoryDao = remember(appDatabase) { appDatabase.categoryDao() }

    val payees by payeeDao.observeAllActive().collectAsState(initial = emptyList())
    val categories by categoryDao.observeAllActive().collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var step by rememberSaveable { mutableStateOf(AddStep.Type) }
    var selectedType by rememberSaveable { mutableStateOf<TxnType?>(null) }
    var selectedPayeeId by rememberSaveable { mutableStateOf<Long?>(null) }
    var payeeQuery by rememberSaveable { mutableStateOf("") }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var transactedAt by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var amountError by rememberSaveable { mutableStateOf<String?>(null) }

    // Resolve the category type (INCOME or EXPENSE) based on selected transaction type
    val categoryTypeForCurrentTxn = remember(selectedType) {
        when (selectedType) {
            TxnType.INCOME, TxnType.DUE_FROM -> TxnType.INCOME
            TxnType.EXPENSE, TxnType.DUE_TO -> TxnType.EXPENSE
            null -> null
        }
    }

    // Filter categories by the resolved type
    val filteredCategories = remember(categories, categoryTypeForCurrentTxn) {
        if (categoryTypeForCurrentTxn == null) {
            emptyList()
        } else {
            categories
                .filter { it.txnType == categoryTypeForCurrentTxn }
                .sortedWith(compareBy<CategoryEntity> { it.sortOrder }.thenBy { it.id })
        }
    }

    // Get default category ID for the current type
    val defaultCategoryId = remember(filteredCategories) {
        filteredCategories.firstOrNull { it.isDefault }?.id
            ?: filteredCategories.firstOrNull()?.id
    }

    LaunchedEffect(defaultCategoryId, selectedCategoryId, categoryTypeForCurrentTxn) {
        if (selectedCategoryId == null && defaultCategoryId != null) {
            selectedCategoryId = defaultCategoryId
        }
    }

    LaunchedEffect(step) {
        when (step) {
            AddStep.Type,
            AddStep.Payee -> {
                if (sheetState.currentValue != SheetValue.PartiallyExpanded) {
                    sheetState.partialExpand()
                }
            }

            AddStep.Details -> {
                if (sheetState.currentValue != SheetValue.Expanded) {
                    sheetState.expand()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
            .padding(horizontal = 4.dp)
            .navigationBarsPadding()
    ) {
        when (step) {
            AddStep.Type -> {
                TypeChooserStep(
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onTypeSelected = { type ->
                        selectedType = type
                        amountError = null
                        if (type == TxnType.DUE_TO || type == TxnType.DUE_FROM) {
                            step = AddStep.Payee
                        } else {
                            selectedPayeeId = null
                            step = AddStep.Details
                        }
                    }
                )
            }

            AddStep.Payee -> {
                PayeeChooserStep(
                    payees = payees,
                    query = payeeQuery,
                    onQueryChange = { payeeQuery = it },
                    onBack = {
                        selectedType = null
                        step = AddStep.Type
                    },
                    onPayeeSelected = { payeeId ->
                        selectedPayeeId = payeeId
                        amountError = null
                        step = AddStep.Details
                    },
                    modifier = Modifier
                        .fillMaxHeight(0.5f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            AddStep.Details -> {
                val type = selectedType
                if (type == null) {
                    onDismissRequest()
                    return@ModalBottomSheet
                }
                DetailsStep(
                    txnType = type,
                    amountInput = amountInput,
                    amountError = amountError,
                    transactedAt = transactedAt,
                    selectedPayee = payees.firstOrNull { it.id == selectedPayeeId },
                    categories = filteredCategories,
                    selectedCategoryId = selectedCategoryId,
                    onAmountChange = {
                        amountInput = it.filter { ch -> ch.isDigit() || ch == '.' }
                        amountError = null
                    },
                    onCategorySelected = { selectedCategoryId = it },
                    onDateClick = {
                        showDatePicker(
                            context = context,
                            initialMillis = transactedAt,
                            onDatePicked = { transactedAt = it }
                        )
                    },
                    onTimeClick = {
                        showTimePicker(
                            context = context,
                            initialMillis = transactedAt,
                            onTimePicked = { transactedAt = it }
                        )
                    },
                    onClose = onDismissRequest,
                    onSave = {
                        val parsedAmountMinor = parseAmountToMinor(amountInput)
                        if (parsedAmountMinor == null || parsedAmountMinor <= 0L) {
                            amountError = "Enter a valid amount"
                            return@DetailsStep
                        }

                        if ((type == TxnType.DUE_TO || type == TxnType.DUE_FROM) && selectedPayeeId == null) {
                            amountError = "Choose a payee"
                            return@DetailsStep
                        }

                        coroutineScope.launch {
                            val now = System.currentTimeMillis()
                            val resolvedCategoryId = appDatabase.withTransaction {
                                when {
                                    selectedCategoryId != null -> selectedCategoryId!!
                                    defaultCategoryId != null -> defaultCategoryId
                                    categoryTypeForCurrentTxn != null -> createDefaultCategory(categoryDao, categoryTypeForCurrentTxn)
                                    else -> createDefaultCategory(categoryDao, TxnType.EXPENSE)
                                }
                            }

                            transactionDao.upsert(
                                TransactionEntity(
                                    amount = parsedAmountMinor,
                                    type = type,
                                    transactedAt = transactedAt,
                                    createdAt = now,
                                    updatedAt = now,
                                    categoryId = resolvedCategoryId,
                                    payeeId = if (type == TxnType.DUE_TO || type == TxnType.DUE_FROM) {
                                        selectedPayeeId
                                    } else {
                                        null
                                    },
                                    accountId = null,
                                    note = null,
                                    receiptUri = null
                                )
                            )
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight(0.95f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .imePadding()
                        .navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun TypeChooserStep(
    onTypeSelected: (TxnType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(all = 16.dp)
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .zIndex(1f)
                        .padding(8.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .zIndex(0f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeCard(
                    title = "Income",
                    emoji = "💸",
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeSelected(TxnType.INCOME) }
                )
                TypeCard(
                    title = "Expense",
                    emoji = "🙁",
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeSelected(TxnType.EXPENSE) }
                )
            }
        }

        Column {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(all = 16.dp)
            ) {
                Text(
                    text = "Dues",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .zIndex(1f)
                        .padding(8.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .zIndex(0f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeCard(
                    title = "Due To",
                    emoji = "🧎",
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeSelected(TxnType.DUE_TO) }
                )
                TypeCard(
                    title = "Due From",
                    emoji = "🏃",
                    modifier = Modifier.weight(1f),
                    onClick = { onTypeSelected(TxnType.DUE_FROM) }
                )
            }
        }
    }
}

@Composable
private fun TypeCard(
    title: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji)
            Text(
                text = "  $title",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PayeeChooserStep(
    payees: List<PayeeEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onPayeeSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedByRecent = remember(payees) { payees.sortedByDescending { it.createdAt } }
    val filtered = remember(sortedByRecent, query) {
        if (query.isBlank()) {
            sortedByRecent
        } else {
            sortedByRecent.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(text = "Choose Payee", style = MaterialTheme.typography.titleLarge)
            Text(text = " ")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sortedByRecent.take(5).forEach { payee ->
                Surface(
                    modifier = Modifier
                        .size(58.dp)
                        .clickable { onPayeeSelected(payee.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    BoxedInitials(name = payee.name)
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            label = { Text("Search payees") }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filtered.take(8).forEach { payee ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPayeeSelected(payee.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = payee.name,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxedInitials(name: String) {
    val trimmed = name.trim()
    val initials = if (trimmed.isBlank()) {
        "?"
    } else {
        trimmed.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DetailsStep(
    txnType: TxnType,
    amountInput: String,
    amountError: String?,
    transactedAt: Long,
    selectedPayee: PayeeEntity?,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onAmountChange: (String) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = remember(transactedAt) { formatDate(transactedAt) }
    val timeText = remember(transactedAt) { formatTime(transactedAt) }
    val showCategory = txnType != TxnType.DUE_FROM
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState, enabled = !isCategoryDropdownExpanded),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clickable(onClick = onClose),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Button(onClick = onSave, shape = RoundedCornerShape(12.dp)) {
                Text("Save")
            }
        }

        if (selectedPayee != null) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("Payee: ${selectedPayee.name}") },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Text(
            text = when (txnType) {
                TxnType.INCOME -> "Income"
                TxnType.EXPENSE -> "Expense"
                TxnType.DUE_TO -> "Due To"
                TxnType.DUE_FROM -> "Due From"
            },
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = amountInput,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                if (amountError != null) {
                    Text(text = amountError)
                }
            },
            isError = amountError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.displaySmall.copy(textAlign = TextAlign.Center),
            label = { Text("Amount") }
        )

        if (showCategory) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall
            )
            CategoryPicker(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.fillMaxWidth(),
                onExpanded = { isExpanded -> isCategoryDropdownExpanded = isExpanded }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDateClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(dateText)
            }
            Button(
                onClick = onTimeClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(timeText)
            }
        }
    }
}

private fun parseAmountToMinor(input: String): Long? {
    val parsed = input.trim().toBigDecimalOrNull() ?: return null
    return try {
        parsed.multiply(BigDecimal(100)).longValueExact()
    } catch (_: ArithmeticException) {
        null
    }
}

private fun formatDate(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(formatter)
}

private fun formatTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(formatter)
}

private fun showDatePicker(
    context: Context,
    initialMillis: Long,
    onDatePicked: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val currentDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(initialMillis),
                ZoneId.systemDefault()
            )
            val merged = LocalDateTime.of(
                year,
                month + 1,
                dayOfMonth,
                currentDateTime.hour,
                currentDateTime.minute
            )
            onDatePicked(
                merged.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showTimePicker(
    context: Context,
    initialMillis: Long,
    onTimePicked: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val currentDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(initialMillis),
                ZoneId.systemDefault()
            )
            val merged = LocalDateTime.of(
                currentDateTime.year,
                currentDateTime.month,
                currentDateTime.dayOfMonth,
                hourOfDay,
                minute
            )
            onTimePicked(
                merged.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}

private suspend fun createDefaultCategory(categoryDao: CategoryDao, txnType: TxnType): Long {
    return categoryDao.upsert(
        CategoryEntity(
            name = "Uncategorized",
            emoji = "TAG",
            colorHex = "#9E9E9E",
            txnType = txnType,
            sortOrder = 0,
            isDefault = true
        )
    )
}

@Preview
@Composable
fun PreviewAddTransactionFlow() {
    AddTransactionFlow(
        onDismissRequest = {}
    )
}
