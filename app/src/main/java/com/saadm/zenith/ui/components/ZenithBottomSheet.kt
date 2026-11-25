package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/**
 * Bottom card with rounded top corners and a grab handle. With [fill] it takes the full height
 * and [ColumnScope] content can spread itself out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenithBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = zenithColors().surface,
        dragHandle = { ZenithGrabHandle() }
    ) {
        BoxWithConstraints(Modifier.imePadding()) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .then(if (fill) Modifier.heightIn(min = maxHeight) else Modifier)
                    .padding(horizontal = ZenithSpacing.space4)
                    .padding(bottom = ZenithSpacing.space4),
                verticalArrangement = if (fill) Arrangement.SpaceBetween else Arrangement.spacedBy(ZenithSpacing.space3),
                content = content
            )
        }
    }
}

/**
 * The one popup used for forms and confirmations: title, optional message and fields, then a
 * full-width action with a quieter dismiss under it.
 */
@Composable
fun ActionSheet(
    title: String,
    onDismiss: () -> Unit,
    message: String? = null,
    confirmLabel: String? = null,
    onConfirm: () -> Unit = {},
    enabled: Boolean = true,
    destructive: Boolean = false,
    dismissLabel: String = "Cancel",
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val colors = zenithColors()
    ZenithBottomSheet(onDismiss) {
        Text(title, style = ZenithType.heading, color = colors.ink, modifier = Modifier.semantics { heading() })
        if (message != null) Text(message, style = ZenithType.body, color = colors.inkMuted)
        content()
        Column(
            modifier = Modifier.padding(top = ZenithSpacing.space2),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)
        ) {
            if (confirmLabel != null) {
                PrimaryButton(confirmLabel, onConfirm, Modifier.fillMaxWidth(), enabled, destructive)
            }
            SecondaryButton(dismissLabel, onDismiss, Modifier.fillMaxWidth())
        }
    }
}

/** The 36 x 4dp grab handle at the top of a sheet. */
@Composable
fun ZenithGrabHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ZenithSpacing.space3),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(zenithColors().border)
        )
    }
}
