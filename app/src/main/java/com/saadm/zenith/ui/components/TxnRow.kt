package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saadm.zenith.data.entity.TxnType
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

@Composable
fun TxnRow(
    amount: Long,
    type: TxnType,
    transactedAt: Long,
    categoryId: Long,
    note: String?,
    currency: String,
    modifier: Modifier = Modifier,
    style: TxnRowStyle = TxnRowDefaults.style(),
    onClick: (() -> Unit)? = null,
    categoryEmojiResolver: (Long) -> String? = { _ -> null },
    categoryLabelResolver: (Long) -> String = { id -> id.toString() },
    amountFormatter: (amount: Long, type: TxnType, currency: String) -> String = TxnRowDefaults::formatAmount,
    timeFormatter: (Long) -> String = TxnRowDefaults::formatTime,
    typeLabelResolver: (TxnType) -> String = TxnRowDefaults::shortTypeLabel
) {
    val resolvedCategory = categoryLabelResolver(categoryId)
    val resolvedCategoryEmoji = categoryEmojiResolver(categoryId)
    val badgeLabel = TxnRowDefaults.resolveBadgeLabel(
        categoryEmoji = resolvedCategoryEmoji,
        categoryLabel = resolvedCategory,
        fallback = typeLabelResolver(type)
    )
    val normalizedNote = note?.trim().orEmpty()
    val hasNote = normalizedNote.isNotEmpty()
    val primaryText = if (hasNote) normalizedNote else resolvedCategory
    val secondaryText = if (hasNote) resolvedCategory else null

    val rowModifier = modifier
        .fillMaxWidth()
        .heightIn(min = style.metrics.minHeight)
        .clip(style.shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Surface(
        modifier = rowModifier,
        color = style.colors.container,
        contentColor = style.colors.primaryText,
        shape = style.shape,
        tonalElevation = style.tonalElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.metrics.rowPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(style.metrics.sectionSpacing)
        ) {
            TypeBadge(type = type, label = badgeLabel, style = style)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(style.metrics.textSpacing)
            ) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = style.colors.primaryText
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(style.metrics.metaSpacing)
                ) {
                    Text(
                        text = timeFormatter(transactedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = style.colors.secondaryText,
                        maxLines = 1
                    )

                    if (!secondaryText.isNullOrBlank()) {
                        Text(
                            text = secondaryText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = style.colors.secondaryText
                        )
                    }
                }
            }

            Text(
                text = amountFormatter(amount, type, currency),
                style = MaterialTheme.typography.titleMedium,
                color = style.colors.amountColor(type),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TypeBadge(type: TxnType, label: String, style: TxnRowStyle) {
    Box(
        modifier = Modifier
            .size(style.metrics.badgeSize)
            .clip(RoundedCornerShape(style.metrics.badgeCornerRadius))
            .background(style.colors.badgeBackground(type)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = style.colors.badgeContent(type),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Immutable
data class TxnRowStyle(
    val colors: TxnRowColors,
    val metrics: TxnRowMetrics,
    val shape: RoundedCornerShape,
    val tonalElevation: Dp
)

@Immutable
data class TxnRowColors(
    val container: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val incomeAmount: Color,
    val expenseAmount: Color,
    val neutralAmount: Color,
    val incomeBadgeBackground: Color,
    val expenseBadgeBackground: Color,
    val neutralBadgeBackground: Color,
    val incomeBadgeContent: Color,
    val expenseBadgeContent: Color,
    val neutralBadgeContent: Color
) {
    fun amountColor(type: TxnType): Color = when (type) {
        TxnType.INCOME, TxnType.DUE_FROM -> incomeAmount
        TxnType.EXPENSE, TxnType.DUE_TO -> expenseAmount
    }

    fun badgeBackground(type: TxnType): Color = when (type) {
        TxnType.INCOME, TxnType.DUE_FROM -> incomeBadgeBackground
        TxnType.EXPENSE, TxnType.DUE_TO -> expenseBadgeBackground
    }

    fun badgeContent(type: TxnType): Color = when (type) {
        TxnType.INCOME, TxnType.DUE_FROM -> incomeBadgeContent
        TxnType.EXPENSE, TxnType.DUE_TO -> expenseBadgeContent
    }
}

@Immutable
data class TxnRowMetrics(
    val minHeight: Dp,
    val rowPadding: androidx.compose.foundation.layout.PaddingValues,
    val sectionSpacing: Dp,
    val textSpacing: Dp,
    val metaSpacing: Dp,
    val badgeSize: Dp,
    val badgeCornerRadius: Dp
)

object TxnRowDefaults {
    fun resolveBadgeLabel(categoryEmoji: String?, categoryLabel: String?, fallback: String): String {
        val normalizedEmoji = categoryEmoji?.trim().orEmpty()
        if (normalizedEmoji.isNotBlank()) return normalizedEmoji

        val normalized = categoryLabel?.trim().orEmpty()
        if (normalized.isBlank()) return fallback

        val shorthand = categoryShorthand(normalized)
        return shorthand.ifBlank { fallback }
    }

    private fun categoryShorthand(label: String): String {
        val words = label
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (words.isEmpty()) return ""

        if (words.size == 1) {
            val letters = words.first().filter { it.isLetterOrDigit() }
            return letters.take(2).uppercase(Locale.ROOT)
        }

        val initials = words
            .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
            .take(2)
            .joinToString("")

        return initials.uppercase(Locale.ROOT)
    }


    @Composable
    fun style(
        colors: TxnRowColors = colors(),
        metrics: TxnRowMetrics = metrics(),
        shape: RoundedCornerShape = RoundedCornerShape(14.dp),
        tonalElevation: Dp = 0.dp
    ): TxnRowStyle = TxnRowStyle(
        colors = colors,
        metrics = metrics,
        shape = shape,
        tonalElevation = tonalElevation
    )

    @Composable
    fun colors(
        container: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        primaryText: Color = MaterialTheme.colorScheme.onSurface,
        secondaryText: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        incomeAmount: Color = Color(0xFF1B8A3A),
        expenseAmount: Color = Color(0xFFC62828),
        neutralAmount: Color = MaterialTheme.colorScheme.onSurface,
        incomeBadgeBackground: Color = Color(0xFFD9F6E2),
        expenseBadgeBackground: Color = Color(0xFFFFE0E0),
        neutralBadgeBackground: Color = MaterialTheme.colorScheme.surface,
        incomeBadgeContent: Color = Color(0xFF116A2E),
        expenseBadgeContent: Color = Color(0xFF8E1C1C),
        neutralBadgeContent: Color = MaterialTheme.colorScheme.onSurface
    ): TxnRowColors = TxnRowColors(
        container = container,
        primaryText = primaryText,
        secondaryText = secondaryText,
        incomeAmount = incomeAmount,
        expenseAmount = expenseAmount,
        neutralAmount = neutralAmount,
        incomeBadgeBackground = incomeBadgeBackground,
        expenseBadgeBackground = expenseBadgeBackground,
        neutralBadgeBackground = neutralBadgeBackground,
        incomeBadgeContent = incomeBadgeContent,
        expenseBadgeContent = expenseBadgeContent,
        neutralBadgeContent = neutralBadgeContent
    )

    fun metrics(
        minHeight: Dp = 68.dp,
        rowPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 10.dp
        ),
        sectionSpacing: Dp = 10.dp,
        textSpacing: Dp = 5.dp,
        metaSpacing: Dp = 6.dp,
        badgeSize: Dp = 42.dp,
        badgeCornerRadius: Dp = 10.dp
    ): TxnRowMetrics = TxnRowMetrics(
        minHeight = minHeight,
        rowPadding = rowPadding,
        sectionSpacing = sectionSpacing,
        textSpacing = textSpacing,
        metaSpacing = metaSpacing,
        badgeSize = badgeSize,
        badgeCornerRadius = badgeCornerRadius
    )

    fun formatAmount(amount: Long, type: TxnType, currency: String): String {
        // Amount is provided in the smallest currency unit (e.g. cents/paise).
        // Convert to major unit by dividing by 100 and format with 2 decimal places.
        val magnitude = abs(amount)
        val value = magnitude / 100.0
        val sign = when (type) {
            TxnType.INCOME, TxnType.DUE_FROM -> ""
            TxnType.EXPENSE, TxnType.DUE_TO -> "-"
        }

        val formatted = runCatching {
            val nf = NumberFormat.getCurrencyInstance()
            nf.currency = Currency.getInstance(currency.uppercase(Locale.ROOT))
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
            // Format the numeric value (positive) and include currency symbol according to locale
            nf.format(value)
        }.getOrElse {
            // Fallback: format number with 2 decimals and prepend currency symbol or code
            val currencySymbol = runCatching {
                Currency.getInstance(currency.uppercase(Locale.ROOT)).symbol
            }.getOrDefault(currency.uppercase(Locale.ROOT))
            val nf = NumberFormat.getNumberInstance()
            nf.minimumFractionDigits = 2
            nf.maximumFractionDigits = 2
            "$currencySymbol${nf.format(value)}"
        }

        return if (sign.isEmpty()) formatted else "$sign$formatted"
    }

    fun formatTime(transactedAt: Long): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return formatter.format(
            Instant.ofEpochMilli(transactedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        )
    }

    fun shortTypeLabel(type: TxnType): String = when (type) {
        TxnType.INCOME -> "IN"
        TxnType.EXPENSE -> "EX"
        TxnType.DUE_TO -> "DT"
        TxnType.DUE_FROM -> "DF"
    }
}

@Preview(showBackground = true)
@Composable
private fun TxnRowExpensePreview() {
    TxnRow(
        amount = 150,
        type = TxnType.EXPENSE,
        transactedAt = System.currentTimeMillis(),
        categoryId = 12,
            note = "mcdonal",
        currency = "INR",
        categoryLabelResolver = { "Food" },
        modifier = Modifier.padding(12.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun TxnRowIncomePreview() {
    TxnRow(
        amount = 10000,
        type = TxnType.INCOME,
        transactedAt = System.currentTimeMillis(),
        categoryId = 7,
        note = "Salary credited",
        currency = "INR",
        categoryLabelResolver = { "Salary" },
        modifier = Modifier.padding(12.dp)
    )
}

@Preview(showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
private fun TxnRowIncomeNoNotePreview() {
    TxnRow(
        amount = 12000,
        type = TxnType.INCOME,
        transactedAt = System.currentTimeMillis()-100,
        categoryId = 7,
        note = "",
        currency = "INR",
        categoryLabelResolver = { "Salary" },
        modifier = Modifier.padding(12.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun TxnRowUSDPreview() {
    TxnRow(
        amount = 50,
        type = TxnType.EXPENSE,
        transactedAt = System.currentTimeMillis(),
        categoryId = 1,
        note = "dolla dolla",
        currency = "USD",
        categoryLabelResolver = { "whar" },
        modifier = Modifier.padding(12.dp)
    )
}
