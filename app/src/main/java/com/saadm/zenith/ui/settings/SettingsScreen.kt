package com.saadm.zenith.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.saadm.zenith.data.db.DatabaseProvider
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.data.preferences.AppPreferences
import com.saadm.zenith.data.preferences.AppPreferencesStore
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

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
                SettingsDestination.Root -> SettingsRootContent(onOpen = ::navigateTo)

                SettingsDestination.General -> GeneralSettingsContent(
                    appearanceMode = appPreferences.appearanceMode,
                    onAppearanceModeSelected = { mode ->
                        coroutineScope.launch { preferencesStore.updateAppearanceMode(mode) }
                    },
                    notificationsEnabled = appPreferences.notificationsEnabled,
                    onNotificationsEnabledChange = { enabled ->
                        coroutineScope.launch { preferencesStore.updateNotificationsEnabled(enabled) }
                    }
                )

                SettingsDestination.Data -> DataSettingsContent(
                    defaultCurrency = appPreferences.defaultCurrency,
                    onDefaultCurrencySelected = { currency ->
                        coroutineScope.launch { preferencesStore.updateDefaultCurrency(currency) }
                    },
                    onCategoriesClick = { navigateTo(SettingsDestination.Categories) }
                )

                SettingsDestination.Qol -> QolSettingsContent(
                    transitionDurationMillis = normalizeTransitionDurationMillis(appPreferences.transitionDurationMillis),
                    onTransitionDurationSelected = { duration ->
                        coroutineScope.launch {
                            preferencesStore.updateTransitionDurationMillis(
                                normalizeTransitionDurationMillis(duration)
                            )
                        }
                    },
                    transitionStyle = appPreferences.transitionStyle,
                    onTransitionStyleSelected = { style ->
                        coroutineScope.launch { preferencesStore.updateTransitionStyle(style) }
                    },
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
private fun SettingsRootContent(onOpen: (SettingsDestination) -> Unit) {
    Text(
        text = "Primary sections",
        style = MaterialTheme.typography.labelLarge
    )

    SettingsSectionCard(
        title = "General",
        subtitle = "Appearance, Notifications",
        onClick = { onOpen(SettingsDestination.General) }
    )
    SettingsSectionCard(
        title = "Data",
        subtitle = "Default Currency, Categories, Import/Export",
        onClick = { onOpen(SettingsDestination.Data) }
    )
    SettingsSectionCard(
        title = "QOL",
        subtitle = "Animations, Haptics, Reports, Labs",
        onClick = { onOpen(SettingsDestination.Qol) }
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GeneralSettingsContent(
    appearanceMode: String,
    onAppearanceModeSelected: (String) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit
) {
    // Appearance
    Text(
        text = "Appearance",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = "Choose how Zenith should look.",
        style = MaterialTheme.typography.bodyMedium
    )
    AppearanceModeOption.entries.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAppearanceModeSelected(option.value) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = appearanceMode.equals(option.value, ignoreCase = true),
                onClick = { onAppearanceModeSelected(option.value) }
            )
            Text(text = option.label)
        }
    }

    Spacer(modifier = Modifier.padding(8.dp))

    // Notifications
    Text(
        text = "Notifications",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = "Enable reminders and smart nudges.",
        style = MaterialTheme.typography.bodyMedium
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNotificationsEnabledChange(!notificationsEnabled) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (notificationsEnabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = onNotificationsEnabledChange
            )
        }
    }
}

@Composable
private fun DataSettingsContent(
    defaultCurrency: String,
    onDefaultCurrencySelected: (String) -> Unit,
    onCategoriesClick: () -> Unit
) {
    val normalizedSelection = defaultCurrency.uppercase()
    var customCurrency by remember(normalizedSelection) {
        mutableStateOf(
            if (normalizedSelection in DEFAULT_CURRENCY_OPTIONS) "" else normalizedSelection
        )
    }

    // Default Currency
    Text(
        text = "Default Currency",
        style = MaterialTheme.typography.labelLarge
    )
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
                onClick = { onDefaultCurrencySelected(currency) },
                label = { Text(currency) }
            )
        }
    }
    OutlinedTextField(
        value = customCurrency,
        onValueChange = {
            customCurrency = it.uppercase().take(4)
            if (customCurrency.length in 3..4) {
                onDefaultCurrencySelected(customCurrency)
            }
        },
        label = { Text("Custom currency") },
        singleLine = true
    )

    Spacer(modifier = Modifier.padding(8.dp))

    // Categories
    Text(
        text = "Categories",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = "Add, edit, and delete categories.",
        style = MaterialTheme.typography.bodyMedium
    )
    Button(
        onClick = onCategoriesClick,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text("Manage categories")
    }

    Spacer(modifier = Modifier.padding(8.dp))

    // Import / Export
    Text(
        text = "Import / Export",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = "Move data in and out of Zenith.",
        style = MaterialTheme.typography.bodyMedium
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { println("[settings] export requested") }) {
            Text("Export")
        }
        Button(onClick = { println("[settings] import requested") }) {
            Text("Import")
        }
    }
}

@Composable
private fun QolSettingsContent(
    transitionDurationMillis: Int,
    onTransitionDurationSelected: (Int) -> Unit,
    transitionStyle: TransitionStyle,
    onTransitionStyleSelected: (TransitionStyle) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit,
    reportsEnabled: Boolean,
    onReportsEnabledChange: (Boolean) -> Unit,
    labsEnabled: Boolean,
    onLabsEnabledChange: (Boolean) -> Unit
) {
    // Animations
    Text(
        text = "Animations",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = "Duration",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    TransitionDurationOptions.forEach { duration ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTransitionDurationSelected(duration) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = transitionDurationMillis == duration,
                onClick = { onTransitionDurationSelected(duration) }
            )
            Text(text = "$duration ms")
        }
    }
    Text(
        text = "Style",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
    TransitionStyle.entries.forEach { style ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTransitionStyleSelected(style) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = transitionStyle == style,
                onClick = { onTransitionStyleSelected(style) }
            )
            Text(text = style.label)
        }
    }

    Spacer(modifier = Modifier.padding(8.dp))

    // Haptics
    Text(
        text = "Haptics",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = "Use subtle vibration feedback for key actions.",
        style = MaterialTheme.typography.bodyMedium
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHapticsEnabledChange(!hapticsEnabled) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (hapticsEnabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = hapticsEnabled,
                onCheckedChange = onHapticsEnabledChange
            )
        }
    }

    Spacer(modifier = Modifier.padding(8.dp))

    // Reports
    Text(
        text = "Reports",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = "Enable detailed trend cards and summaries.",
        style = MaterialTheme.typography.bodyMedium
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onReportsEnabledChange(!reportsEnabled) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (reportsEnabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = reportsEnabled,
                onCheckedChange = onReportsEnabledChange
            )
        }
    }

    Spacer(modifier = Modifier.padding(8.dp))

    // Labs
    Text(
        text = "Labs",
        style = MaterialTheme.typography.labelLarge
    )
    Text(
        text = "Opt into experimental features.",
        style = MaterialTheme.typography.bodyMedium
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLabsEnabledChange(!labsEnabled) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (labsEnabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = labsEnabled,
                onCheckedChange = onLabsEnabledChange
            )
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

    categories
        .sortedWith(compareBy<CategoryEntity> { it.sortOrder }.thenBy { it.id })
        .forEach { category ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
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

                Text(
                    text = "Emoji",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORY_EMOJI_OPTIONS.forEach { option ->
                        FilterChip(
                            selected = normalizeCategoryEmoji(emoji) == option,
                            onClick = { emoji = option },
                            label = { Text(option) }
                        )
                    }
                }

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
}

private enum class SettingsDestination(val route: String, val title: String) {
    Root("root", "Settings"),
    General("general", "General"),
    Data("data", "Data"),
    Qol("qol", "QOL"),
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

private val DEFAULT_CURRENCY_OPTIONS = listOf("USD", "EUR", "GBP", "PKR", "INR")

private val CATEGORY_EMOJI_OPTIONS = listOf(
    "🏷️", "🍔", "🚌", "💡", "🛍️", "🏠", "🎬", "💊", "📚", "💰"
)

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