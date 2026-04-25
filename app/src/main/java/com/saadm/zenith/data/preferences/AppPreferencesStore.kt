package com.saadm.zenith.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_APPEARANCE_MODE
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_CURRENCY
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_HAPTICS_ENABLED
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_LABS_ENABLED
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_NOTIFICATIONS_ENABLED
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_REPORTS_ENABLED
import com.saadm.zenith.ui.settings.DEFAULT_TRANSITION_DURATION_MILLIS
import com.saadm.zenith.ui.settings.DEFAULT_TRANSITION_STYLE
import com.saadm.zenith.ui.settings.TransitionStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

data class AppPreferences(
    val transitionDurationMillis: Int = DEFAULT_TRANSITION_DURATION_MILLIS,
    val transitionStyle: TransitionStyle = DEFAULT_TRANSITION_STYLE,
    val appearanceMode: String = DEFAULT_APPEARANCE_MODE,
    val notificationsEnabled: Boolean = DEFAULT_NOTIFICATIONS_ENABLED,
    val defaultCurrency: String = DEFAULT_CURRENCY,
    val hapticsEnabled: Boolean = DEFAULT_HAPTICS_ENABLED,
    val reportsEnabled: Boolean = DEFAULT_REPORTS_ENABLED,
    val labsEnabled: Boolean = DEFAULT_LABS_ENABLED
)

class AppPreferencesStore(private val context: Context) {

    val preferencesFlow: Flow<AppPreferences> = context.appPreferencesDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            val transitionDurationMillis =
                preferences[PreferenceKeys.TRANSITION_DURATION_MILLIS] ?: DEFAULT_TRANSITION_DURATION_MILLIS
            val transitionStyle =
                TransitionStyle.fromStorageValue(preferences[PreferenceKeys.TRANSITION_STYLE])
            val appearanceMode =
                preferences[PreferenceKeys.APPEARANCE_MODE] ?: DEFAULT_APPEARANCE_MODE
            val notificationsEnabled =
                preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: DEFAULT_NOTIFICATIONS_ENABLED
            val defaultCurrency =
                preferences[PreferenceKeys.DEFAULT_CURRENCY] ?: DEFAULT_CURRENCY
            val hapticsEnabled =
                preferences[PreferenceKeys.HAPTICS_ENABLED] ?: DEFAULT_HAPTICS_ENABLED
            val reportsEnabled =
                preferences[PreferenceKeys.REPORTS_ENABLED] ?: DEFAULT_REPORTS_ENABLED
            val labsEnabled =
                preferences[PreferenceKeys.LABS_ENABLED] ?: DEFAULT_LABS_ENABLED

            AppPreferences(
                transitionDurationMillis = transitionDurationMillis,
                transitionStyle = transitionStyle,
                appearanceMode = appearanceMode,
                notificationsEnabled = notificationsEnabled,
                defaultCurrency = defaultCurrency,
                hapticsEnabled = hapticsEnabled,
                reportsEnabled = reportsEnabled,
                labsEnabled = labsEnabled
            )
        }

    suspend fun updateTransitionDurationMillis(durationMillis: Int) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferenceKeys.TRANSITION_DURATION_MILLIS] = durationMillis
        }
    }

    suspend fun updateTransitionStyle(style: TransitionStyle) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferenceKeys.TRANSITION_STYLE] = style.storageValue
        }
    }

    suspend fun updateAppearanceMode(mode: String) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferenceKeys.APPEARANCE_MODE] = mode
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateDefaultCurrency(currency: String) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferenceKeys.DEFAULT_CURRENCY] = currency.uppercase()
        }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferenceKeys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun updateReportsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferenceKeys.REPORTS_ENABLED] = enabled
        }
    }

    suspend fun updateLabsEnabled(enabled: Boolean) {
        context.appPreferencesDataStore.edit { preferences ->
            preferences[PreferenceKeys.LABS_ENABLED] = enabled
        }
    }

    companion object {
        const val DEFAULT_APPEARANCE_MODE = "SYSTEM"
        const val DEFAULT_NOTIFICATIONS_ENABLED = true
        const val DEFAULT_CURRENCY = "INR"
        const val DEFAULT_HAPTICS_ENABLED = true
        const val DEFAULT_REPORTS_ENABLED = true
        const val DEFAULT_LABS_ENABLED = false
    }

    private object PreferenceKeys {
        val TRANSITION_DURATION_MILLIS = intPreferencesKey("transition_duration_millis")
        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
        val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val REPORTS_ENABLED = booleanPreferencesKey("reports_enabled")
        val LABS_ENABLED = booleanPreferencesKey("labs_enabled")
    }
}

