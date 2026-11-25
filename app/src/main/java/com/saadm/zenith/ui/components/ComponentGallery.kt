package com.saadm.zenith.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithPalette
import com.saadm.zenith.ui.theme.ZenithSpacing
import com.saadm.zenith.ui.theme.ZenithTheme
import com.saadm.zenith.ui.theme.ZenithType
import com.saadm.zenith.ui.theme.zenithColors

/** Every shared component in one scrolling page. The previews below render it in each palette. */
@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    val colors = zenithColors()
    var segment by remember { mutableIntStateOf(0) }
    var filterOn by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(ZenithSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space6)
    ) {
        GallerySection("Buttons") {
            PrimaryButton(text = "Save expense", onClick = {})
            PrimaryButton(text = "Save expense", onClick = {}, enabled = false)
            SecondaryButton(text = "Cancel", onClick = {})
            ZenithTextButton(text = "See all", onClick = {})
        }

        GallerySection("Chips and controls") {
            Row(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
                FilterChip(label = "All", selected = filterOn, onClick = { filterOn = true })
                FilterChip(label = "Income", selected = !filterOn, onClick = { filterOn = false })
            }
            SegmentedControl(
                options = listOf("Week", "Month", "Year"),
                selectedIndex = segment,
                onSelect = { segment = it }
            )
            ZenithSwitch(
                label = "Notifications",
                checked = notifications,
                onCheckedChange = { notifications = it }
            )
        }

        GallerySection("Status") {
            Row(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
                StatusTag(text = "Settled", tone = StatusTone.Positive)
                StatusTag(text = "Overdue", tone = StatusTone.Negative)
                StatusTag(text = "Pending", tone = StatusTone.Caution)
                StatusTag(text = "Draft", tone = StatusTone.Neutral)
            }
        }

        GallerySection("Avatars") {
            Row(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)) {
                Avatar(name = "Asha", size = AvatarSize.Small)
                Avatar(name = "Ravi", size = AvatarSize.Medium)
                Avatar(name = "Meera", size = AvatarSize.XLarge)
            }
            AvatarStack(names = listOf("Asha", "Ravi", "Meera", "Dev", "Kiran"))
        }

        GallerySection("Progress") {
            ProgressTrack(progress = 0.6f, description = "Budget 60 percent used", paceTick = 0.5f)
            ProgressTrack(progress = 1.1f, description = "Over budget", fillColor = colors.negative)
        }

        GallerySection("Lists") {
            SectionHeader(title = "Recent", actionLabel = "See all", onAction = {})
            ZenithCard {
                ListRow(
                    title = "Lunch",
                    subtitle = "Today, 1:20 PM",
                    leading = { Avatar(name = "Lunch") },
                    trailing = { AmountText(amount = 42_000, tone = AmountTone.Negative) },
                    trailingCaption = "Expense",
                    onClick = {}
                )
            }
        }

        GallerySection("Month") {
            MonthSwitcher(label = "Oct 2026", onPrevious = {}, onNext = {}, nextEnabled = false)
        }

        GallerySection("Amounts") {
            AmountText(amount = 10_707_000, style = ZenithType.display)
            AmountText(amount = 1_200_000, signed = true, tone = AmountTone.Positive)
            AmountText(amount = -42_050, tone = AmountTone.Negative)
            AmountText(amount = 2_430_000, style = ZenithType.body)
        }

        GallerySection("Card and sheet") {
            ZenithCard {
                Text(text = "Card content sits on the raised surface.", style = ZenithType.body, color = colors.ink)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceRaised, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(bottom = ZenithSpacing.space4)
            ) {
                ZenithGrabHandle()
                Text(
                    text = "Sheet content (ZenithBottomSheet shows this in a modal)",
                    style = ZenithType.body,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(horizontal = ZenithSpacing.space4)
                )
            }
        }

        GallerySection("Empty state") {
            EmptyState(
                title = "Start with one entry",
                body = "Transactions show up here, grouped by day.",
                example = {
                    ZenithCard {
                        ListRow(title = "Example: groceries", subtitle = "Tomorrow", trailing = {
                            AmountText(amount = 0, tone = AmountTone.Neutral)
                        })
                    }
                },
                actionLabel = "Add your first transaction",
                onAction = {}
            )
        }
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ZenithSpacing.space3)) {
        Text(text = title, style = ZenithType.caption, color = zenithColors().inkMuted)
        content()
    }
}

@Preview(name = "Gallery - Light", widthDp = 390, heightDp = 2400, showBackground = true)
@Composable
private fun ComponentGalleryLight() {
    ZenithTheme(mode = ThemeMode.Light) { ComponentGallery() }
}

@Preview(name = "Gallery - Dark", widthDp = 390, heightDp = 2400, showBackground = true)
@Composable
private fun ComponentGalleryDark() {
    ZenithTheme(mode = ThemeMode.Dark) { ComponentGallery() }
}

@Preview(name = "Gallery - Monochrome", widthDp = 390, heightDp = 2400, showBackground = true)
@Composable
private fun ComponentGalleryMonochrome() {
    ZenithTheme(mode = ThemeMode.Light, palette = ZenithPalette.Monochrome) { ComponentGallery() }
}

@Preview(name = "Gallery - Monochrome dark", widthDp = 390, heightDp = 2400, showBackground = true)
@Composable
private fun ComponentGalleryMonochromeDark() {
    ZenithTheme(mode = ThemeMode.Dark, palette = ZenithPalette.Monochrome) { ComponentGallery() }
}
