package com.saadm.zenith.ui.components

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Heading for a group of rows, with an optional trailing link. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ZenithType.subheading,
            color = zenithColors().ink,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (actionLabel != null && onAction != null) {
            ZenithTextButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Leading slot, title and subtitle, then a trailing slot (amount) and an optional caption under it.
 * Clickable only when [onClick] is set.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    trailingCaption: String? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = zenithColors()
    val large = LocalDensity.current.fontScale >= 1.5f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier.semantics(mergeDescendants = true) {})
            .padding(vertical = ZenithSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)
    ) {
        leading?.invoke()
        val text: @Composable () -> Unit = {
            Text(
                text = title,
                style = ZenithType.bodyStrong,
                color = colors.ink,
                maxLines = if (large) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ZenithType.caption,
                    color = colors.inkMuted,
                    maxLines = if (large) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        val end: @Composable () -> Unit = {
            trailing?.invoke()
            if (trailingCaption != null) {
                Text(
                    text = trailingCaption,
                    style = ZenithType.caption,
                    color = colors.inkMuted,
                    textAlign = if (large) TextAlign.Start else TextAlign.End
                )
            }
        }
        // At large font sizes the two columns would squeeze each other, so they stack.
        Column(modifier = Modifier.weight(1f)) {
            text()
            if (large) end()
        }
        if (!large && (trailing != null || trailingCaption != null)) {
            Column(horizontalAlignment = Alignment.End) { end() }
        }
    }
}
