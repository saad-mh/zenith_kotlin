package com.saadm.zenith.ui.common

import com.saadm.zenith.ui.components.Lu
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Back button, title and an optional control at the end. Sits above a screen's scroll. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(
            start = ZenithSpacing.space1,
            top = ZenithSpacing.space2,
            end = ZenithSpacing.space4,
            bottom = ZenithSpacing.space2
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(painterResource(Lu.lucide_ic_chevron_left), contentDescription = "Back", tint = zenithColors().ink)
        }
        // Wraps at large font sizes, so the title is never broken mid-word.
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = ZenithType.heading,
                color = zenithColors().ink,
                modifier = Modifier.semantics { heading() }
            )
            Row(content = trailing)
        }
    }
}
