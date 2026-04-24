package com.saadm.zenith.ui.settings

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.saadm.zenith.data.db.DatabaseProvider
import com.saadm.zenith.data.entity.CategoryEntity
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    selectedDurationMillis: Int,
    onDurationSelected: (Int) -> Unit,
    selectedStyle: TransitionStyle,
    onStyleSelected: (TransitionStyle) -> Unit
) {
    val context = LocalContext.current
    val appDatabase = remember(context.applicationContext) {
        DatabaseProvider.getInstance(context.applicationContext)
    }
    val categoryDao = remember(appDatabase) { appDatabase.categoryDao() }
    val categories by categoryDao.observeAllActive().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Navigation animation",
                style = MaterialTheme.typography.titleMedium
            )

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

            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "Add, edit, or remove categories.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = { showCreateDialog = true },
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
                                IconButton(onClick = { editingCategory = category }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit ${category.name}"
                                    )
                                }
                                IconButton(
                                    onClick = {
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
                                ) {
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

private enum class CategoryApplicableOption(val value: String, val label: String) {
    Expense("EXPENSE", "Expense"),
    Income("INCOME", "Income"),
    Both("BOTH", "Both")
}

private fun buildCategoryMeta(category: CategoryEntity): String {
    val target = when (category.applicableTo.uppercase()) {
        "EXPENSE" -> "Expense"
        "INCOME" -> "Income"
        else -> "Both"
    }
    return if (category.isDefault) "$target • Default" else target
}

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
        Color(android.graphics.Color.parseColor(normalized))
    } catch (_: IllegalArgumentException) {
        Color(0xFF9E9E9E)
    }
}

