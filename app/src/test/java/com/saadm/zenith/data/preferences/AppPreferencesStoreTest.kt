package com.saadm.zenith.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.saadm.zenith.domain.model.NumberFormat
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.model.WeekStart
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// In-memory stand-in for the file-backed DataStore. The file-based one fails to rename on Windows JVMs.
private class InMemoryPreferencesStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.value = transform(state.value)
        return state.value
    }
}

class AppPreferencesStoreTest {

    @Test
    fun defaultsAreSystemThemeAndColourPalette() = runBlocking {
        val prefs = AppPreferencesStore(InMemoryPreferencesStore()).preferencesFlow.first()

        assertEquals(ThemeMode.System, prefs.themeMode)
        assertEquals(ZenithPalette.Colour, prefs.palette)
    }

    @Test
    fun themeModeAndPaletteRoundTrip() = runBlocking {
        val store = AppPreferencesStore(InMemoryPreferencesStore())

        store.updateThemeMode(ThemeMode.Dark)
        store.updatePalette(ZenithPalette.Monochrome)
        val prefs = store.preferencesFlow.first()

        assertEquals(ThemeMode.Dark, prefs.themeMode)
        assertEquals(ZenithPalette.Monochrome, prefs.palette)
    }

    @Test
    fun choicesSurviveARestart() = runBlocking {
        val saved = InMemoryPreferencesStore()
        AppPreferencesStore(saved).apply {
            updateThemeMode(ThemeMode.Light)
            updatePalette(ZenithPalette.Monochrome)
        }

        // A new store over the saved preferences, as after process death.
        val prefs = AppPreferencesStore(InMemoryPreferencesStore(saved.data.first())).preferencesFlow.first()

        assertEquals(ThemeMode.Light, prefs.themeMode)
        assertEquals(ZenithPalette.Monochrome, prefs.palette)
    }

    @Test
    fun financeAndReminderDefaults() = runBlocking {
        val prefs = AppPreferencesStore(InMemoryPreferencesStore()).preferencesFlow.first()

        assertEquals(NumberFormat.INDIAN, prefs.numberFormat)
        assertEquals(WeekStart.MONDAY, prefs.weekStart)
        assertEquals(1, prefs.monthStartDay)
        assertEquals(false, prefs.dailyReminderEnabled)
        assertEquals(20 * 60, prefs.dailyReminderMinutes)
        assertEquals(null, prefs.defaultAccountId)
        assertEquals(SplitMode.EQUAL, prefs.defaultSplitMode)
        assertEquals(true, prefs.fillCategoryFromPayee)
        assertEquals(true, prefs.hideInRecents)
    }

    @Test
    fun financeAndReminderSettingsRoundTrip() = runBlocking {
        val store = AppPreferencesStore(InMemoryPreferencesStore())

        store.updateNumberFormat(NumberFormat.INTERNATIONAL)
        store.updateWeekStart(WeekStart.SUNDAY)
        store.updateMonthStartDay(15)
        store.updateDailyReminder(enabled = true, minutesAfterMidnight = 7 * 60 + 30)
        store.updateSettleUpNudges(true)
        store.updateDefaultAccountId(42L)
        store.updateDefaultSplitMode(SplitMode.BY_ITEMS)
        store.updateFillCategoryFromPayee(false)
        store.updateAppLock(enabled = true, graceSeconds = 120)
        store.updateHideInRecents(false)
        val prefs = store.preferencesFlow.first()

        assertEquals(NumberFormat.INTERNATIONAL, prefs.numberFormat)
        assertEquals(WeekStart.SUNDAY, prefs.weekStart)
        assertEquals(15, prefs.monthStartDay)
        assertEquals(true, prefs.dailyReminderEnabled)
        assertEquals(7 * 60 + 30, prefs.dailyReminderMinutes)
        assertEquals(true, prefs.settleUpNudgesEnabled)
        assertEquals(42L, prefs.defaultAccountId)
        assertEquals(SplitMode.BY_ITEMS, prefs.defaultSplitMode)
        assertEquals(false, prefs.fillCategoryFromPayee)
        assertEquals(true, prefs.appLockEnabled)
        assertEquals(120, prefs.appLockGraceSeconds)
        assertEquals(false, prefs.hideInRecents)
    }

    @Test
    fun monthStartDayIsCappedAndDefaultAccountCanBeCleared() = runBlocking {
        val store = AppPreferencesStore(InMemoryPreferencesStore())

        store.updateMonthStartDay(40)
        assertEquals(28, store.preferencesFlow.first().monthStartDay)
        store.updateMonthStartDay(0)
        assertEquals(1, store.preferencesFlow.first().monthStartDay)

        store.updateDefaultAccountId(7L)
        store.updateDefaultAccountId(null)
        assertEquals(null, store.preferencesFlow.first().defaultAccountId)
    }
}
