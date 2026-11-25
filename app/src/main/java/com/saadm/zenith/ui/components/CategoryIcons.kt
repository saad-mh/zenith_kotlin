package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** One icon a category can have. [key] is what the database stores, so it must never change. */
data class CategoryIcon(val key: String, val label: String, val image: ImageVector)

// The first seven keys are the values the seeded categories already hold, so they need no migration.
val categoryIcons: List<CategoryIcon> = listOf(
    CategoryIcon("TAG", "Tag", Icons.AutoMirrored.Rounded.Label),
    CategoryIcon("DINE", "Food", Icons.Rounded.Restaurant),
    CategoryIcon("COMMUTE", "Transport", Icons.Rounded.DirectionsBus),
    CategoryIcon("BILL", "Bills", Icons.AutoMirrored.Rounded.ReceiptLong),
    CategoryIcon("MORE", "Other", Icons.Rounded.MoreHoriz),
    CategoryIcon("💰", "Money", Icons.Rounded.Payments),
    CategoryIcon("🎁", "Gift", Icons.Rounded.CardGiftcard),
    CategoryIcon("GROCERIES", "Groceries", Icons.Rounded.ShoppingCart),
    CategoryIcon("SHOPPING", "Shopping", Icons.Rounded.ShoppingBag),
    CategoryIcon("HOME", "Home", Icons.Rounded.Home),
    CategoryIcon("HEALTH", "Health", Icons.Rounded.LocalHospital),
    CategoryIcon("EDUCATION", "Education", Icons.Rounded.School),
    CategoryIcon("TRAVEL", "Travel", Icons.Rounded.Flight),
    CategoryIcon("FUN", "Entertainment", Icons.Rounded.Movie),
    CategoryIcon("COFFEE", "Coffee", Icons.Rounded.LocalCafe),
    CategoryIcon("FITNESS", "Fitness", Icons.Rounded.FitnessCenter),
    CategoryIcon("PETS", "Pets", Icons.Rounded.Pets),
    CategoryIcon("PHONE", "Phone", Icons.Rounded.Phone),
    CategoryIcon("SAVINGS", "Savings", Icons.Rounded.Savings),
    CategoryIcon("WORK", "Work", Icons.Rounded.Work),
)

fun categoryIcon(key: String?): CategoryIcon? = categoryIcons.firstOrNull { it.key == key }

/**
 * What to draw when [key] is not in the set: an emoji someone stored earlier is shown as it is,
 * anything else (no key, or a key from a newer version) becomes the name's initial.
 */
fun fallbackGlyph(key: String?, name: String): String =
    key?.takeIf { it.isNotBlank() && it.none { c -> c.isLetterOrDigit() } }
        ?: name.trim().take(1).uppercase().ifEmpty { "?" }

/** A category's icon in a neutral circle. Decorative: the name is always written beside it. */
@Composable
fun CategoryBadge(key: String?, name: String, modifier: Modifier = Modifier) {
    val colors = zenithColors()
    val icon = categoryIcon(key)
    Box(
        modifier = modifier.size(40.dp).background(colors.surfaceSunken, CircleShape).clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) Icon(icon.image, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
        else Text(fallbackGlyph(key, name), style = ZenithType.bodyStrong, color = colors.ink)
    }
}
