package com.saadm.zenith.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    val transitionStyle: TransitionStyle = DEFAULT_TRANSITION_STYLE
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

            AppPreferences(
                transitionDurationMillis = transitionDurationMillis,
                transitionStyle = transitionStyle
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

    private object PreferenceKeys {
        val TRANSITION_DURATION_MILLIS = intPreferencesKey("transition_duration_millis")
        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
    }
}

