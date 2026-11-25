package com.saadm.zenith.ui.add

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.data.entity.CategoryEntity
import com.saadm.zenith.ui.components.ActionSheet
import com.saadm.zenith.ui.components.CategoryBadge
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.components.ListRow
import com.saadm.zenith.ui.components.Lu
import com.saadm.zenith.ui.components.categoryIcon
import com.saadm.zenith.ui.theme.zenithColors

/** The category an entry will be saved under: the picked one if it fits the type, else the default. */
fun effectiveCategory(categories: List<CategoryEntity>, pickedId: Long?): CategoryEntity? =
    categories.firstOrNull { it.id == pickedId } ?: categories.firstOrNull { it.isDefault }

/** One chip showing the category; tapping it lists every category that fits the entry. */
@Composable
fun CategoryChooser(categories: List<CategoryEntity>, pickedId: Long?, onPick: (Long) -> Unit) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val current = effectiveCategory(categories, pickedId) ?: return
    FilterChip(
        current.name,
        false,
        { choosing = true },
        Modifier.semantics { contentDescription = "Category: ${current.name}. Change" },
        icon = categoryIcon(current.emoji)?.image ?: Lu.lucide_ic_tag
    )
    if (choosing) {
        ActionSheet(title = "Category", dismissLabel = "Close", onDismiss = { choosing = false }) {
            categories.forEach { category ->
                ListRow(
                    title = category.name,
                    leading = { CategoryBadge(category.emoji, category.name) },
                    trailing = if (category.id == current.id) {
                        {
                            Icon(
                                painterResource(Lu.lucide_ic_check),
                                contentDescription = "Selected",
                                tint = zenithColors().link,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else null,
                    onClick = {
                        onPick(category.id)
                        choosing = false
                    }
                )
            }
        }
    }
}
