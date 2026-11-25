package com.saadm.zenith.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saadm.zenith.testing.TestGraph
import com.saadm.zenith.testing.awaitUntil
import com.saadm.zenith.ui.add.AddTransactionFlow
import com.saadm.zenith.ui.components.FilterChip
import com.saadm.zenith.ui.settings.DeleteAllSheet
import com.saadm.zenith.ui.theme.ZenithTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h914dp")
class ComposeUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val graph = TestGraph()

    private fun click(node: SemanticsNodeInteraction) {
        compose.waitForIdle()
        node.performSemanticsAction(SemanticsActions.OnClick)
    }

    @After
    fun tearDown() = graph.close()

    @Test
    fun typingAnAmountAndTappingTheTickSavesIt() {
        var closed = false
        val vm = graph.add()
        compose.setContent { ZenithTheme { AddTransactionFlow(onDismissRequest = { closed = true }, viewModel = vm) } }
        click(compose.onNodeWithContentDescription("4"))
        click(compose.onNodeWithContentDescription("2"))
        click(compose.onNodeWithContentDescription("Save expense · ₹42"))
        // The write finishes on Room's thread and is delivered through the main looper.
        awaitUntil { vm.saved.value != null }
        compose.waitForIdle()
        assertTrue(closed)
        assertEquals(4_200L, runBlocking { graph.transactions.observeAllActive().first() }.single().amount)
    }

    @Test
    fun deleteEverythingStaysOffUntilDeleteIsTyped() {
        var confirmed: String? = null
        compose.setContent { ZenithTheme { DeleteAllSheet(onDismiss = {}, onConfirm = { confirmed = it }) } }
        compose.onNodeWithText("Delete everything").assertIsNotEnabled()
        compose.onNode(hasText("Type DELETE")).performTextInput("DELETE")
        click(compose.onNodeWithText("Delete everything"))
        compose.waitForIdle()
        assertEquals("DELETE", confirmed)
    }

    @Test
    fun aFilterChipReportsItselfSelected() {
        compose.setContent {
            ZenithTheme {
                var on by remember { mutableStateOf(false) }
                FilterChip("Income", on, { on = !on })
            }
        }
        compose.onNodeWithText("Income").assertIsDisplayed().performClick()
        compose.onNodeWithText("Income").assertIsSelected()
    }
}
