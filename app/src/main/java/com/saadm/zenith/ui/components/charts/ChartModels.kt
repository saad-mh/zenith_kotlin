package com.saadm.zenith.ui.components.charts

import androidx.compose.ui.graphics.Color
import com.saadm.zenith.ui.theme.ZenithColors

// Plain data for the charts. Money is in paise, like the rest of the app. Charts never read a ViewModel.

data class MonthBar(val label: String, val amount: Long)

data class RankedItem(val label: String, val amount: Long)

/** One column of a stacked chart. [segments] run bottom to top; at most four. */
data class StackedColumn(val label: String, val segments: List<Long>)

data class GroupedBar(val label: String, val first: Long, val second: Long)

/** Net cash flow for one period. Positive is a gain, negative a loss. */
data class NetFlowBar(val label: String, val net: Long)

data class ShareSlice(val label: String, val amount: Long)

data class BudgetRing(val label: String, val spent: Long, val limit: Long)

/** Series colour c1..c6, wrapping if a chart has more series than colours. */
internal fun ZenithColors.chart(index: Int): Color = chartColors[index % chartColors.size]
