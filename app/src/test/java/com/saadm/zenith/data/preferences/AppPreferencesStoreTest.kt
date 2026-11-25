package com.saadm.zenith.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
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
}
