package com.saadm.zenith.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.toColorInt
import com.saadm.zenith.data.entity.CategoryEntity

@Composable
fun CategoryPicker(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onExpanded: (Boolean) -> Unit = {}
) {
    val isExpanded = remember { androidx.compose.runtime.mutableStateOf(false) }
    val selectedCategory = remember(selectedCategoryId, categories) {
        categories.firstOrNull { it.id == selectedCategoryId }
    }
    val onExpandedUpdated = rememberUpdatedState(onExpanded)

    LaunchedEffect(isExpanded.value) {
        onExpandedUpdated.value(isExpanded.value)
    }

    Column(modifier = modifier) {
        // Selected item display
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isExpanded.value = !isExpanded.value }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                    .background(categoryColor(selectedCategory?.colorHex ?: "#E0E0E0"))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Text(
                        text = categoryEmoji(selectedCategory?.emoji ?: "??"),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .zIndex(1f)
                            .padding(4.dp)
                    )
                    Box (
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                categoryColor(selectedCategory?.colorHex ?: "#E0E0E0").copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .zIndex(0f)
                    )
                }
                if (selectedCategory != null) {
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = selectedCategory.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = "Select category",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (isExpanded.value) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expanded list
        AnimatedVisibility(
            visible = isExpanded.value,
            enter = fadeIn(animationSpec = tween(200, easing = LinearEasing)) +
                    slideInVertically(
                        animationSpec = tween(200, easing = LinearEasing),
                        initialOffsetY = { -it }
                    ),
            exit = fadeOut(animationSpec = tween(200, easing = LinearEasing)) +
                    slideOutVertically(
                        animationSpec = tween(200, easing = LinearEasing),
                        targetOffsetY = { -it }
                    ),
            modifier = Modifier.animateContentSize()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategoryId == category.id
                        val rowColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        } else {
                            Color.Transparent
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(rowColor)
                                .clickable {
                                    onCategorySelected(category.id)
                                    isExpanded.value = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.size(10.dp))
                            Box {
                                Text(
                                    text = categoryEmoji(category.emoji),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .zIndex(1f)
                                        .padding(4.dp)
                                )
                                Box (
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            categoryColor(category.colorHex).copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .zIndex(0f)
                                )
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun categoryEmoji(raw: String): String {
    return when (raw.trim().uppercase()) {
        "TAG" -> "🏷️"
        "DINE" -> "🍔"
        "COMMUTE" -> "🚌"
        "BILL" -> "💡"
        "MORE" -> "🛍️"
        else -> raw.ifBlank { "??" }
    }
}

private fun categoryColor(raw: String): Color {
    val cleaned = raw.trim().removePrefix("#")
    val valid = cleaned.length == 6 && cleaned.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
    val normalized = if (valid) "#${cleaned.uppercase()}" else "#9E9E9E"
    return try {
        Color(normalized.toColorInt())
    } catch (_: IllegalArgumentException) {
        Color(0xFF9E9E9E)
    }
}
