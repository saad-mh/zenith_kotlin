package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.zenithColors

/** Bottom sheet with a 12dp top radius, a grab handle and the theme scrim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenithBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = zenithColors().surfaceRaised,
        dragHandle = { ZenithGrabHandle() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = ZenithSpacing.space4, vertical = ZenithSpacing.space4),
            verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3),
            content = content
        )
    }
}

/** The 36 x 4dp grab handle at the top of a sheet. */
@Composable
fun ZenithGrabHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ZenithSpacing.space2),
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
