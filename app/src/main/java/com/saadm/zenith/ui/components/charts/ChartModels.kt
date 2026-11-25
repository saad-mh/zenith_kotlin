package com.saadm.zenith.ui.components.charts

import androidx.compose.ui.graphics.Color
import com.saadm.zenith.ui.theme.ZenithColors

// Plain data for the charts. Money is in paise, like the rest of the app. Charts never read a ViewModel.

data class MonthBar(val label: String, val amount: Long)

data class ShareSlice(val label: String, val amount: Long)

/** Series colour c1..c6, wrapping if a chart has more series than colours. */
internal fun ZenithColors.chart(index: Int): Color = chartColors[index % chartColors.size]
