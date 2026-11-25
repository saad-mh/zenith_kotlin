package com.saadm.zenith.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.domain.model.MoneyFormatter
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithPalette
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithTheme
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Every chart in the kit with sample data. The previews render it in each palette. */
@Composable
fun ChartGallery(modifier: Modifier = Modifier) {
    val colors = zenithColors()
    val slices = listOf(
        ShareSlice("Rent", 3_200_000),
        ShareSlice("Food", 1_450_000),
        ShareSlice("Travel", 620_000),
        ShareSlice("Fun", 310_000),
    )
    val total = slices.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(ZenithSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space6)
    ) {
        ChartSection("Monthly spending") {
            MonthlyBars(
                bars = listOf(
                    MonthBar("May", 8_200_000), MonthBar("Jun", 9_100_000), MonthBar("Jul", 7_400_000),
                    MonthBar("Aug", 10_300_000), MonthBar("Sep", 8_900_000), MonthBar("Oct", 6_100_000),
                ),
                average = 8_500_000,
                projection = 9_400_000,
            )
        }
        ChartSection("Ranked categories") {
            RankedBars(
                items = slices.sortedByDescending { it.amount }.map { RankedItem(it.label, it.amount) }
            )
        }
        ChartSection("Stacked by week") {
            StackedBars(
                columns = listOf(
                    StackedColumn("W1", listOf(900_000, 400_000, 200_000)),
                    StackedColumn("W2", listOf(1_100_000, 350_000, 150_000, 80_000)),
                    StackedColumn("W3", listOf(800_000, 500_000)),
                    StackedColumn("W4", listOf(1_000_000, 300_000, 120_000)),
                ),
                segmentLabels = listOf("Needs", "Food", "Fun", "Other"),
            )
        }
        ChartSection("Income and spending") {
            GroupedBars(
                groups = listOf(
                    GroupedBar("May", 12_000_000, 8_200_000),
                    GroupedBar("Jun", 11_500_000, 9_100_000),
                    GroupedBar("Jul", 12_000_000, 7_400_000),
                ),
                firstLabel = "Income",
                secondLabel = "Spending",
            )
        }
        ChartSection("Net cash flow") {
            NetFlowBars(
                bars = listOf(
                    NetFlowBar("May", 3_800_000),
                    NetFlowBar("Jun", 2_400_000),
                    NetFlowBar("Jul", -420_000),
                    NetFlowBar("Aug", -1_100_000),
                    NetFlowBar("Sep", 1_200_000),
                ),
            )
        }
        ChartSection("This month against last") {
            CumulativeLine(
                current = listOf(0, 120_000, 260_000, 340_000, 510_000, 640_000, 720_000).map { it.toLong() },
                previous = listOf(0, 150_000, 300_000, 420_000, 560_000, 700_000, 810_000, 900_000).map { it.toLong() },
            )
        }
        ChartSection("Overview sparkline") {
            Sparkline(values = listOf(120_000, 90_000, 240_000, 180_000, 310_000, 260_000).map { it.toLong() })
        }
        ChartSection("Share") { ShareBar(slices) }
        ChartSection("Donut") {
            Donut(slices = slices, centreTotal = MoneyFormatter.format(total).text, centreCaption = "this month")
        }
        ChartSection("Split") { SplitPie(slices) }
        ChartSection("Budget gauge") {
            Gauge(spent = 7_200_000, limit = 10_000_000, paceFraction = 0.6f)
        }
        ChartSection("Budget rings") {
            BudgetRings(
                rings = listOf(
                    BudgetRing("Food", 4_800_000, 6_000_000),
                    BudgetRing("Travel", 1_300_000, 1_000_000),
                    BudgetRing("Fun", 0, 500_000),
                ),
            )
        }
    }
}

@Composable
private fun ChartSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)) {
        Text(text = title, style = ZenithType.caption, color = zenithColors().inkMuted)
        content()
    }
}

@Preview(name = "Charts - Light", widthDp = 390, heightDp = 2200, showBackground = true)
@Composable
private fun ChartGalleryLight() {
    ZenithTheme(mode = ThemeMode.Light) { ChartGallery() }
}

@Preview(name = "Charts - Dark", widthDp = 390, heightDp = 2200, showBackground = true)
@Composable
private fun ChartGalleryDark() {
    ZenithTheme(mode = ThemeMode.Dark) { ChartGallery() }
}

@Preview(name = "Charts - Monochrome", widthDp = 390, heightDp = 2200, showBackground = true)
@Composable
private fun ChartGalleryMonochrome() {
    ZenithTheme(mode = ThemeMode.Light, palette = ZenithPalette.Monochrome) { ChartGallery() }
}

@Preview(name = "Charts - Monochrome dark", widthDp = 390, heightDp = 2200, showBackground = true)
@Composable
private fun ChartGalleryMonochromeDark() {
    ZenithTheme(mode = ThemeMode.Dark, palette = ZenithPalette.Monochrome) { ChartGallery() }
}
