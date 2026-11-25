package com.saadm.zenith.ui

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.captureRoboImage
import com.saadm.zenith.ui.budgets.BudgetsPreview
import com.saadm.zenith.ui.budgets.sessionQuipSalt
import com.saadm.zenith.ui.home.OverviewPreview
import com.saadm.zenith.ui.insights.InsightsPreview
import com.saadm.zenith.ui.people.PeoplePreview
import com.saadm.zenith.ui.settings.SettingsPreview
import com.saadm.zenith.ui.transactions.TxnListPreview
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Each screen's preview (light, dark and monochrome, with fixed data) as an image in
 * src/testDebug/screenshots. Record: -Proborazzi.record. Compare: -Proborazzi.verify, on the same
 * OS that recorded them, since text rendering differs between systems.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h2400dp-420dpi")
class ScreenshotTest {

    private fun shot(name: String, content: @Composable () -> Unit) =
        captureRoboImage("src/testDebug/screenshots/$name.png") { content() }

    @Test
    fun overview() = shot("overview") { OverviewPreview() }

    @Test
    fun insights() = shot("insights") { InsightsPreview() }

    @Test
    fun budgets() {
        sessionQuipSalt = 0
        shot("budgets") { BudgetsPreview() }
    }

    @Test
    fun transactions() = shot("transactions") { TxnListPreview() }

    @Test
    fun people() = shot("people") { PeoplePreview() }

    @Test
    fun settings() = shot("settings") { SettingsPreview() }
}
