package com.saadm.zenith.ui.common

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Height covered by the floating pill (or the system bar when there is no pill). Content scrolls
 * behind it, so every scrolling screen adds this to the end of its scroll.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }
