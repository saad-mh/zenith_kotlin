package com.saadm.zenith.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.ui.graphics.vector.ImageVector
import com.saadm.zenith.data.db.DatabaseProvider
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.preferences.AppPreferences
import com.saadm.zenith.data.preferences.AppPreferencesStore
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import androidx.emoji2.emojipicker.EmojiPickerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val appDatabase = remember(context.applicationContext) {
        DatabaseProvider.getInstance(context.applicationContext)
    }
    val categoryDao = remember(appDatabase) { appDatabase.categoryDao() }
    val categories by categoryDao.observeAllActive().collectAsState(initial = emptyList())

    val preferencesStore = remember(context.applicationContext) {
        AppPreferencesStore(context.applicationContext)
    }
    val appPreferences by preferencesStore.preferencesFlow.collectAsState(initial = AppPreferences())
    val coroutineScope = rememberCoroutineScope()

    var destinationStack by rememberSaveable {
        mutableStateOf(listOf(SettingsDestination.Root.route))
    }
    val currentDestination = remember(destinationStack) {
        SettingsDestination.fromRoute(destinationStack.last())
    }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(currentDestination) {
        if (currentDestination != SettingsDestination.Categories) {
            showCreateDialog = false
            editingCategory = null
        }
    }

    fun navigateTo(destination: SettingsDestination) {
        destinationStack = destinationStack + destination.route
    }

    fun navigateBack() {
        if (destinationStack.size > 1) {
            destinationStack = destinationStack.dropLast(1)
        }
    }

    BackHandler(enabled = destinationStack.size > 1) {
        navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentDestination.title) },
                navigationIcon = {
                    if (destinationStack.size > 1) {
                        IconButton(onClick = ::navigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentDestination) {
                SettingsDestination.Root -> SettingsRootContent(
                    appearanceMode = appPreferences.appearanceMode,
                    onAppearanceClick = { navigateTo(SettingsDestination.Appearance) },
                    notificationsEnabled = appPreferences.notificationsEnabled,
                    onNotificationsEnabledChange = { enabled ->
                        coroutineScope.launch { preferencesStore.updateNotificationsEnabled(enabled) }
                    },
                    defaultCurrency = appPreferences.defaultCurrency,
                    onCurrencyClick = { navigateTo(SettingsDestination.DefaultCurrency) },
                    animationSummary = "${normalizeTransitionDurationMillis(appPreferences.transitionDurationMillis)} ms • ${appPreferences.transitionStyle.label}",
                    onAnimationsClick = { navigateTo(SettingsDestination.Animations) },
                    hapticsEnabled = appPreferences.hapticsEnabled,
                    onHapticsEnabledChange = { enabled ->
                        coroutineScope.launch { preferencesStore.updateHapticsEnabled(enabled) }
                    },
                    reportsEnabled = appPreferences.reportsEnabled,
                    onReportsEnabledChange = { enabled ->
                        coroutineScope.launch { preferencesStore.updateReportsEnabled(enabled) }
                    },
                    labsEnabled = appPreferences.labsEnabled,
                    onLabsEnabledChange = { enabled ->
                        coroutineScope.launch { preferencesStore.updateLabsEnabled(enabled) }
                    },
                    onCategoriesClick = { navigateTo(SettingsDestination.Categories) },
                    onImportClick = { println("[settings] import requested") },
                    onExportClick = { println("[settings] export requested") }
                )

                SettingsDestination.Appearance -> AppearanceSettingsContent(
                    selectedMode = appPreferences.appearanceMode,
                    onModeSelected = { mode ->
                        coroutineScope.launch { preferencesStore.updateAppearanceMode(mode) }
                    }
                )

                SettingsDestination.DefaultCurrency -> CurrencySettingsContent(
                    selectedCurrency = appPreferences.defaultCurrency,
                    onSelected = { currency ->
                        coroutineScope.launch { preferencesStore.updateDefaultCurrency(currency) }
                    }
                )

                SettingsDestination.Animations -> AnimationSettingsContent(
                    selectedDurationMillis = normalizeTransitionDurationMillis(appPreferences.transitionDurationMillis),
                    onDurationSelected = { duration ->
                        coroutineScope.launch {
                            preferencesStore.updateTransitionDurationMillis(
                                normalizeTransitionDurationMillis(duration)
                            )
                        }
                    },
                    selectedStyle = appPreferences.transitionStyle,
                    onStyleSelected = { style ->
                        coroutineScope.launch { preferencesStore.updateTransitionStyle(style) }
                    }
                )

                SettingsDestination.Categories -> CategoryManagementContent(
                    categories = categories,
                    onCreateClick = { showCreateDialog = true },
                    onEditClick = { editingCategory = it },
                    onDeleteClick = { category ->
                        coroutineScope.launch {
                            categoryDao.update(category.copy(isDeleted = true, isDefault = false))
                            if (category.isDefault) {
                                categories
                                    .firstOrNull { it.id != category.id }
                                    ?.let { replacement ->
                                        categoryDao.update(replacement.copy(isDefault = true))
                                    }
                            }
                        }
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        CategoryEditorDialog(
            title = "Add category",
            initialName = "",
            initialEmoji = "🏷️",
            initialColorHex = "#9E9E9E",
            initialApplicableTo = "EXPENSE",
            initialIsDefault = false,
            onDismiss = { showCreateDialog = false },
            onSave = { name, emoji, colorHex, applicableTo, isDefault ->
                coroutineScope.launch {
                    if (isDefault) {
                        categories.filter { it.isDefault }.forEach { existingDefault ->
                            categoryDao.update(existingDefault.copy(isDefault = false))
                        }
                    }

                    val nextSortOrder = (categories.maxOfOrNull { it.sortOrder } ?: -1) + 1
                    categoryDao.upsert(
                        CategoryEntity(
                            name = name.trim(),
                            emoji = normalizeCategoryEmoji(emoji),
                            colorHex = normalizeColorHex(colorHex),
                            applicableTo = applicableTo,
                            sortOrder = nextSortOrder,
                            isDefault = isDefault,
                            isDeleted = false
                        )
                    )
                    showCreateDialog = false
                }
            }
        )
    }

    editingCategory?.let { category ->
        CategoryEditorDialog(
            title = "Edit category",
            initialName = category.name,
            initialEmoji = normalizeCategoryEmoji(category.emoji),
            initialColorHex = normalizeColorHex(category.colorHex),
            initialApplicableTo = category.applicableTo,
            initialIsDefault = category.isDefault,
            onDismiss = { editingCategory = null },
            onSave = { name, emoji, colorHex, applicableTo, isDefault ->
                coroutineScope.launch {
                    if (isDefault && !category.isDefault) {
                        categories.filter { it.isDefault && it.id != category.id }
                            .forEach { existingDefault ->
                                categoryDao.update(existingDefault.copy(isDefault = false))
                            }
                    }

                    categoryDao.update(
                        category.copy(
                            name = name.trim(),
                            emoji = normalizeCategoryEmoji(emoji),
                            colorHex = normalizeColorHex(colorHex),
                            applicableTo = applicableTo,
                            isDefault = isDefault
                        )
                    )

                    if (category.isDefault && !isDefault) {
                        categories.firstOrNull { it.id != category.id }?.let { fallback ->
                            categoryDao.update(fallback.copy(isDefault = true))
                        }
                    }

                    editingCategory = null
                }
            }
        )
    }
}

@Composable
private fun SettingsRootContent(
    appearanceMode: String,
    onAppearanceClick: () -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    defaultCurrency: String,
    onCurrencyClick: () -> Unit,
    animationSummary: String,
    onAnimationsClick: () -> Unit,
    hapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit,
    reportsEnabled: Boolean,
    onReportsEnabledChange: (Boolean) -> Unit,
    labsEnabled: Boolean,
    onLabsEnabledChange: (Boolean) -> Unit,
    onCategoriesClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Text(
        text = "General",
        style = MaterialTheme.typography.labelLarge
    )

    SettingsGroup {
        SettingsNavigationRow(
            icon = Icons.Rounded.DarkMode,
            title = "Appearance",
            value = appearanceModeLabel(appearanceMode),
            onClick = onAppearanceClick
        )
        SettingsToggleRow(
            icon = Icons.Rounded.Notifications,
            title = "Notifications",
            checked = notificationsEnabled,
            onCheckedChange = onNotificationsEnabledChange
        )
        SettingsNavigationRow(
            icon = Icons.Rounded.GraphicEq,
            title = "Animations",
            value = animationSummary,
            onClick = onAnimationsClick
        )
        SettingsToggleRow(
            icon = Icons.Rounded.GraphicEq,
            title = "Haptics",
            checked = hapticsEnabled,
            onCheckedChange = onHapticsEnabledChange
        )
    }

    Text(
        text = "Data",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 12.dp)
    )
    SettingsGroup {
        SettingsNavigationRow(
            icon = Icons.Rounded.Category,
            title = "Categories",
            value = null,
            onClick = onCategoriesClick
        )
        SettingsNavigationRow(
            icon = Icons.Rounded.CurrencyExchange,
            title = "Currency",
            value = defaultCurrency,
            onClick = onCurrencyClick
        )
        SettingsActionRow(
            icon = Icons.Rounded.Download,
            title = "Import Data",
            onClick = onImportClick
        )
        SettingsActionRow(
            icon = Icons.Rounded.Upload,
            title = "Export Data",
            onClick = onExportClick
        )
    }

    Text(
        text = "QOL",
        style = MaterialTheme.typography.labelLarge
    )

    SettingsGroup {
        SettingsToggleRow(
            icon = Icons.Rounded.AutoAwesome,
            title = "Reports",
            checked = reportsEnabled,
            onCheckedChange = onReportsEnabledChange
        )
        SettingsToggleRow(
            icon = Icons.Rounded.AutoAwesome,
            title = "Labs",
            checked = labsEnabled,
            onCheckedChange = onLabsEnabledChange
        )
    }

    Text(
        text = "More",
        style = MaterialTheme.typography.labelLarge
    )

    SettingsGroup {
        SettingsNavigationRow(
            icon = Icons.Rounded.AutoAwesome,
            title = "About",
            onClick = { },
            value = null
        )
    }


}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon = icon)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon = icon)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    SettingsNavigationRow(
        icon = icon,
        title = title,
        value = null,
        onClick = onClick
    )
}

@Composable
private fun SettingsRowIcon(icon: ImageVector) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AppearanceSettingsContent(
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    Text(
        text = "Choose how Zenith should look.",
        style = MaterialTheme.typography.bodyMedium
    )

    AppearanceModeOption.entries.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onModeSelected(option.value) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedMode.equals(option.value, ignoreCase = true),
                onClick = { onModeSelected(option.value) }
            )
            Text(text = option.label)
        }
    }
}

@Composable
private fun CurrencySettingsContent(
    selectedCurrency: String,
    onSelected: (String) -> Unit
) {
    val normalizedSelection = selectedCurrency.uppercase()
    var customCurrency by remember(normalizedSelection) {
        mutableStateOf(
            if (normalizedSelection in DEFAULT_CURRENCY_OPTIONS) "" else normalizedSelection
        )
    }

    Text(
        text = "Select your default currency for new records.",
        style = MaterialTheme.typography.bodyMedium
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DEFAULT_CURRENCY_OPTIONS.forEach { currency ->
            FilterChip(
                selected = normalizedSelection == currency,
                onClick = { onSelected(currency) },
                label = { Text(currency) }
            )
        }
    }

    OutlinedTextField(
        value = customCurrency,
        onValueChange = {
            customCurrency = it.uppercase().take(4)
            if (customCurrency.length in 3..4) {
                onSelected(customCurrency)
            }
        },
        label = { Text("Custom currency") },
        singleLine = true
    )
}

@Composable
private fun AnimationSettingsContent(
    selectedDurationMillis: Int,
    onDurationSelected: (Int) -> Unit,
    selectedStyle: TransitionStyle,
    onStyleSelected: (TransitionStyle) -> Unit
) {
    Text(
        text = "Duration",
        style = MaterialTheme.typography.labelLarge
    )
    TransitionDurationOptions.forEach { duration ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDurationSelected(duration) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedDurationMillis == duration,
                onClick = { onDurationSelected(duration) }
            )
            Text(text = "$duration ms")
        }
    }

    Text(
        text = "Style",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp)
    )
    TransitionStyle.entries.forEach { style ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStyleSelected(style) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedStyle == style,
                onClick = { onStyleSelected(style) }
            )
            Text(text = style.label)
        }
    }
}

@Composable
private fun CategoryManagementContent(
    categories: List<CategoryEntity>,
    onCreateClick: () -> Unit,
    onEditClick: (CategoryEntity) -> Unit,
    onDeleteClick: (CategoryEntity) -> Unit
) {
    val sortedCategories = remember(categories) {
        categories.sortedWith(compareBy<CategoryEntity> { it.sortOrder }.thenBy { it.id })
    }
    val expenseCategories = remember(sortedCategories) {
        sortedCategories.filter { it.appliesToExpense() }
    }
    val incomeCategories = remember(sortedCategories) {
        sortedCategories.filter { it.appliesToIncome() }
    }

    Text(
        text = "Add, edit, or remove categories.",
        style = MaterialTheme.typography.bodyMedium
    )

    Button(
        onClick = onCreateClick,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text("Add category")
    }

    CategorySection(
        title = "Expense categories",
        categories = expenseCategories,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick
    )

    CategorySection(
        title = "Income categories",
        categories = incomeCategories,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick
    )
}

@Composable
private fun CategorySection(
    title: String,
    categories: List<CategoryEntity>,
    onEditClick: (CategoryEntity) -> Unit,
    onDeleteClick: (CategoryEntity) -> Unit
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (categories.isEmpty()) {
            Text(
                text = "No categories yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            )
        } else {
            Column {
                categories.forEachIndexed { index, category ->
                    CategoryRow(
                        category = category,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick
                    )
                    if (index != categories.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    onEditClick: (CategoryEntity) -> Unit,
    onDeleteClick: (CategoryEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.small,
                color = parseColorHex(category.colorHex).copy(alpha = 0.18f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = normalizeCategoryEmoji(category.emoji))
                }
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = buildCategoryMeta(category),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(22.dp),
                shape = MaterialTheme.shapes.small,
                color = parseColorHex(category.colorHex)
            ) {}
            IconButton(onClick = { onEditClick(category) }) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit ${category.name}"
                )
            }
            IconButton(onClick = { onDeleteClick(category) }) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete ${category.name}"
                )
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    initialColorHex: String,
    initialApplicableTo: String,
    initialIsDefault: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, colorHex: String, applicableTo: String, isDefault: Boolean) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var emoji by remember(initialEmoji) { mutableStateOf(initialEmoji) }
    var colorHex by remember(initialColorHex) { mutableStateOf(initialColorHex) }
    var applicableTo by remember(initialApplicableTo) { mutableStateOf(initialApplicableTo) }
    var isDefault by remember(initialIsDefault) { mutableStateOf(initialIsDefault) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = normalizeCategoryEmoji(emoji),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Emoji") },
                    singleLine = true,
                    supportingText = { Text("Tap Pick to open the emoji picker") },
                    trailingIcon = {
                        TextButton(onClick = { showEmojiPicker = true }) {
                            Text("Pick")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEmojiPicker = true }
                )

                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it },
                    label = { Text("Color hex") },
                    singleLine = true,
                    trailingIcon = {
                        Surface(
                            modifier = Modifier.size(18.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            color = parseColorHex(colorHex)
                        ) {}
                    }
                )

                Text(
                    text = "Applies to",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryApplicableOption.entries.forEach { option ->
                        FilterChip(
                            selected = applicableTo == option.value,
                            onClick = { applicableTo = option.value },
                            label = { Text(option.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDefault = !isDefault },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isDefault,
                        onClick = { isDefault = !isDefault }
                    )
                    Text(text = "Set as default")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, emoji, colorHex, applicableTo, isDefault)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showEmojiPicker) {
        EmojiPickerDialog(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { pickedEmoji ->
                emoji = pickedEmoji
                showEmojiPicker = false
            }
        )
    }
}

@Composable
private fun EmojiPickerDialog(
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick emoji") },
        text = {
            AndroidView(
                factory = { context ->
                    EmojiPickerView(context).apply {
                        setOnEmojiPickedListener { item ->
                            onEmojiSelected(item.emoji)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private enum class SettingsDestination(val route: String, val title: String) {
    Root("root", "Settings"),
    Appearance("appearance", "Appearance"),
    DefaultCurrency("default_currency", "Currency"),
    Animations("animations", "Animations"),
    Categories("categories", "Categories");

    companion object {
        fun fromRoute(route: String): SettingsDestination {
            return entries.firstOrNull { it.route == route } ?: Root
        }
    }
}

private enum class AppearanceModeOption(val value: String, val label: String) {
    System("SYSTEM", "System default"),
    Light("LIGHT", "Light"),
    Dark("DARK", "Dark")
}

private enum class CategoryApplicableOption(val value: String, val label: String) {
    Expense("EXPENSE", "Expense"),
    Income("INCOME", "Income"),
    Both("BOTH", "Both")
}

private fun appearanceModeLabel(rawMode: String): String {
    return AppearanceModeOption.entries
        .firstOrNull { it.value.equals(rawMode, ignoreCase = true) }
        ?.label
        ?: AppearanceModeOption.System.label
}

private fun buildCategoryMeta(category: CategoryEntity): String {
    val target = when (category.applicableTo.uppercase()) {
        "EXPENSE" -> "Expense"
        "INCOME" -> "Income"
        else -> "Both"
    }
    return if (category.isDefault) "$target • Default" else target
}

private val DEFAULT_CURRENCY_OPTIONS = listOf("USD", "EUR", "GBP", "INR")

private fun normalizeCategoryEmoji(raw: String): String {
    return when (raw.trim().uppercase()) {
        "TAG" -> "🏷️"
        "DINE" -> "🍔"
        "COMMUTE" -> "🚌"
        "BILL" -> "💡"
        "MORE" -> "🛍️"
        else -> raw.ifBlank { "🏷️" }
    }
}

private fun CategoryEntity.appliesToExpense(): Boolean {
    return applicableTo.equals("EXPENSE", ignoreCase = true) || applicableTo.equals("BOTH", ignoreCase = true)
}

private fun CategoryEntity.appliesToIncome(): Boolean {
    return applicableTo.equals("INCOME", ignoreCase = true) || applicableTo.equals("BOTH", ignoreCase = true)
}

private fun normalizeColorHex(raw: String): String {
    val cleaned = raw.trim().removePrefix("#")
    val valid = cleaned.length == 6 && cleaned.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
    return if (valid) "#${cleaned.uppercase()}" else "#9E9E9E"
}

private fun parseColorHex(raw: String): Color {
    val normalized = normalizeColorHex(raw)
    return try {
        Color(normalized.toColorInt())
    } catch (_: IllegalArgumentException) {
        Color(0xFF9E9E9E)
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewSettings() {
    SettingsScreen()
}