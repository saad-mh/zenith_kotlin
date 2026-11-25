package com.saadm.zenith.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** One icon a category can have. [key] is what the database stores, so it must never change. */
data class CategoryIcon(val key: String, val label: String, val image: Int)

// The first seven keys are the values the seeded categories already hold, so they need no migration.
val categoryIcons: List<CategoryIcon> = listOf(
    CategoryIcon("TAG", "Tag", Lu.lucide_ic_tag),
    CategoryIcon("DINE", "Food", Lu.lucide_ic_utensils),
    CategoryIcon("COMMUTE", "Transport", Lu.lucide_ic_bus),
    CategoryIcon("BILL", "Bills", Lu.lucide_ic_receipt_text),
    CategoryIcon("MORE", "Other", Lu.lucide_ic_ellipsis),
    CategoryIcon("💰", "Money", Lu.lucide_ic_banknote),
    CategoryIcon("🎁", "Gift", Lu.lucide_ic_gift),
    CategoryIcon("GROCERIES", "Groceries", Lu.lucide_ic_shopping_cart),
    CategoryIcon("SHOPPING", "Shopping", Lu.lucide_ic_shopping_bag),
    CategoryIcon("HOME", "Home", Lu.lucide_ic_house),
    CategoryIcon("HEALTH", "Health", Lu.lucide_ic_heart_pulse),
    CategoryIcon("EDUCATION", "Education", Lu.lucide_ic_graduation_cap),
    CategoryIcon("TRAVEL", "Travel", Lu.lucide_ic_plane),
    CategoryIcon("FUN", "Entertainment", Lu.lucide_ic_clapperboard),
    CategoryIcon("COFFEE", "Coffee", Lu.lucide_ic_coffee),
    CategoryIcon("FITNESS", "Fitness", Lu.lucide_ic_dumbbell),
    CategoryIcon("PETS", "Pets", Lu.lucide_ic_paw_print),
    CategoryIcon("PHONE", "Phone", Lu.lucide_ic_smartphone),
    CategoryIcon("SAVINGS", "Savings", Lu.lucide_ic_piggy_bank),
    CategoryIcon("WORK", "Work", Lu.lucide_ic_briefcase),
)

fun categoryIcon(key: String?): CategoryIcon? = categoryIcons.firstOrNull { it.key == key }

/**
 * What to draw when [key] is not in the set: an emoji someone stored earlier is shown as it is,
 * anything else (no key, or a key from a newer version) becomes the name's initial.
 */
fun fallbackGlyph(key: String?, name: String): String =
    key?.takeIf { it.isNotBlank() && it.none { c -> c.isLetterOrDigit() } }
        ?: name.trim().take(1).uppercase().ifEmpty { "?" }

/** A category's icon in a tinted tile. Decorative: the name is always written beside it. */
@Composable
fun CategoryBadge(key: String?, name: String, modifier: Modifier = Modifier) {
    val tint = zenithColors().avatarTints[Math.floorMod(name.trim().lowercase().hashCode(), 4)]
    val icon = categoryIcon(key)
    Box(
        modifier = modifier.size(40.dp).background(tint.bg, MaterialTheme.shapes.small).clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(painterResource(icon.image), contentDescription = null, tint = tint.fg, modifier = Modifier.size(20.dp))
        else Text(fallbackGlyph(key, name), style = ZenithType.bodyStrong, color = tint.fg)
    }
}
