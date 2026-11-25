package com.saadm.zenith.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/**
 * Title, one line of body text, an optional example row and an optional primary action.
 * Each empty screen says what it will show once there is data.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    example: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = zenithColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ZenithSpacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)
    ) {
        Text(text = title, style = ZenithType.heading, color = colors.ink, textAlign = TextAlign.Center)
        Text(text = body, style = ZenithType.body, color = colors.inkMuted, textAlign = TextAlign.Center)
        example?.invoke()
        if (actionLabel != null && onAction != null) {
            PrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}
