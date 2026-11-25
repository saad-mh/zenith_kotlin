package com.saadm.zenith.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_CURRENCY
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_HAPTICS_ENABLED
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_LABS_ENABLED
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_NOTIFICATIONS_ENABLED
import com.saadm.zenith.data.preferences.AppPreferencesStore.Companion.DEFAULT_REPORTS_ENABLED
import com.saadm.zenith.domain.model.MonthStartDay
import com.saadm.zenith.domain.model.NumberFormat
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.model.WeekStart
import com.saadm.zenith.ui.settings.DEFAULT_TRANSITION_DURATION_MILLIS
import com.saadm.zenith.ui.settings.DEFAULT_TRANSITION_STYLE
import com.saadm.zenith.ui.settings.TransitionStyle
import com.saadm.zenith.ui.theme.ThemeMode
import com.saadm.zenith.ui.theme.ZenithPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

data class AppPreferences(
    val transitionDurationMillis: Int = DEFAULT_TRANSITION_DURATION_MILLIS,
    val transitionStyle: TransitionStyle = DEFAULT_TRANSITION_STYLE,
    val themeMode: ThemeMode = ThemeMode.System,
    val palette: ZenithPalette = ZenithPalette.Colour,
    val notificationsEnabled: Boolean = DEFAULT_NOTIFICATIONS_ENABLED,
    val defaultCurrency: String = DEFAULT_CURRENCY,
    val hapticsEnabled: Boolean = DEFAULT_HAPTICS_ENABLED,
    val reportsEnabled: Boolean = DEFAULT_REPORTS_ENABLED,
    val labsEnabled: Boolean = DEFAULT_LABS_ENABLED,
    val numberFormat: NumberFormat = NumberFormat.INDIAN,
    val weekStart: WeekStart = WeekStart.MONDAY,
    val monthStartDay: Int = 1,
    val dailyReminderEnabled: Boolean = false,
    /** Minutes after midnight. 20:00 by default. */
    val dailyReminderMinutes: Int = 20 * 60,
    val settleUpNudgesEnabled: Boolean = false,
    val defaultAccountId: Long? = null,
    val defaultSplitMode: SplitMode = SplitMode.EQUAL,
    val fillCategoryFromPayee: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockGraceSeconds: Int = 30,
    val hideInRecents: Boolean = true,
    val profileName: String = "",
    val upiId: String = ""
)

class AppPreferencesStore(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.appPreferencesDataStore)

    val preferencesFlow: Flow<AppPreferences> = dataStore.data
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
            // Stored as SYSTEM / LIGHT / DARK and COLOUR / MONOCHROME; unknown values fall back to the defaults.
            val themeMode = ThemeMode.entries
                .firstOrNull { it.name.equals(preferences[PreferenceKeys.APPEARANCE_MODE], ignoreCase = true) }
                ?: ThemeMode.System
            val palette = ZenithPalette.entries
                .firstOrNull { it.name.equals(preferences[PreferenceKeys.PALETTE], ignoreCase = true) }
                ?: ZenithPalette.Colour
            val defaults = AppPreferences()
            AppPreferences(
                transitionDurationMillis = transitionDurationMillis,
                transitionStyle = transitionStyle,
                themeMode = themeMode,
                palette = palette,
                notificationsEnabled = preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: DEFAULT_NOTIFICATIONS_ENABLED,
                defaultCurrency = preferences[PreferenceKeys.DEFAULT_CURRENCY] ?: DEFAULT_CURRENCY,
                hapticsEnabled = preferences[PreferenceKeys.HAPTICS_ENABLED] ?: DEFAULT_HAPTICS_ENABLED,
                reportsEnabled = preferences[PreferenceKeys.REPORTS_ENABLED] ?: DEFAULT_REPORTS_ENABLED,
                labsEnabled = preferences[PreferenceKeys.LABS_ENABLED] ?: DEFAULT_LABS_ENABLED,
                numberFormat = enumOr(preferences[PreferenceKeys.NUMBER_FORMAT], defaults.numberFormat),
                weekStart = enumOr(preferences[PreferenceKeys.WEEK_START], defaults.weekStart),
                monthStartDay = MonthStartDay.coerce(preferences[PreferenceKeys.MONTH_START_DAY] ?: defaults.monthStartDay),
                dailyReminderEnabled = preferences[PreferenceKeys.DAILY_REMINDER_ENABLED] ?: defaults.dailyReminderEnabled,
                dailyReminderMinutes = (preferences[PreferenceKeys.DAILY_REMINDER_MINUTES] ?: defaults.dailyReminderMinutes)
                    .coerceIn(0, 24 * 60 - 1),
                settleUpNudgesEnabled = preferences[PreferenceKeys.SETTLE_UP_NUDGES] ?: defaults.settleUpNudgesEnabled,
                defaultAccountId = preferences[PreferenceKeys.DEFAULT_ACCOUNT_ID],
                defaultSplitMode = enumOr(preferences[PreferenceKeys.DEFAULT_SPLIT_MODE], defaults.defaultSplitMode),
                fillCategoryFromPayee = preferences[PreferenceKeys.FILL_CATEGORY_FROM_PAYEE] ?: defaults.fillCategoryFromPayee,
                appLockEnabled = preferences[PreferenceKeys.APP_LOCK_ENABLED] ?: defaults.appLockEnabled,
                appLockGraceSeconds = preferences[PreferenceKeys.APP_LOCK_GRACE_SECONDS] ?: defaults.appLockGraceSeconds,
                hideInRecents = preferences[PreferenceKeys.HIDE_IN_RECENTS] ?: defaults.hideInRecents,
                profileName = preferences[PreferenceKeys.PROFILE_NAME].orEmpty(),
                upiId = preferences[PreferenceKeys.UPI_ID].orEmpty()
            )
        }

    suspend fun updateTransitionDurationMillis(durationMillis: Int) = set(PreferenceKeys.TRANSITION_DURATION_MILLIS, durationMillis)

    suspend fun updateTransitionStyle(style: TransitionStyle) = set(PreferenceKeys.TRANSITION_STYLE, style.storageValue)

    suspend fun updateThemeMode(mode: ThemeMode) = set(PreferenceKeys.APPEARANCE_MODE, mode.name.uppercase())

    suspend fun updatePalette(palette: ZenithPalette) = set(PreferenceKeys.PALETTE, palette.name.uppercase())

    suspend fun updateNotificationsEnabled(enabled: Boolean) = set(PreferenceKeys.NOTIFICATIONS_ENABLED, enabled)

    suspend fun updateDefaultCurrency(currency: String) = set(PreferenceKeys.DEFAULT_CURRENCY, currency.uppercase())

    suspend fun updateHapticsEnabled(enabled: Boolean) = set(PreferenceKeys.HAPTICS_ENABLED, enabled)

    suspend fun updateReportsEnabled(enabled: Boolean) = set(PreferenceKeys.REPORTS_ENABLED, enabled)

    suspend fun updateLabsEnabled(enabled: Boolean) = set(PreferenceKeys.LABS_ENABLED, enabled)

    suspend fun updateNumberFormat(format: NumberFormat) = set(PreferenceKeys.NUMBER_FORMAT, format.name)

    suspend fun updateWeekStart(start: WeekStart) = set(PreferenceKeys.WEEK_START, start.name)

    suspend fun updateMonthStartDay(day: Int) = set(PreferenceKeys.MONTH_START_DAY, MonthStartDay.coerce(day))

    suspend fun updateDailyReminder(enabled: Boolean, minutesAfterMidnight: Int) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.DAILY_REMINDER_ENABLED] = enabled
            prefs[PreferenceKeys.DAILY_REMINDER_MINUTES] = minutesAfterMidnight.coerceIn(0, 24 * 60 - 1)
        }
    }

    suspend fun updateSettleUpNudges(enabled: Boolean) = set(PreferenceKeys.SETTLE_UP_NUDGES, enabled)

    /** Null clears the default account. */
    suspend fun updateDefaultAccountId(accountId: Long?) {
        dataStore.edit { prefs ->
            if (accountId == null) prefs.remove(PreferenceKeys.DEFAULT_ACCOUNT_ID)
            else prefs[PreferenceKeys.DEFAULT_ACCOUNT_ID] = accountId
        }
    }

    suspend fun updateProfile(name: String, upiId: String) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.PROFILE_NAME] = name.trim()
            prefs[PreferenceKeys.UPI_ID] = upiId.trim()
        }
    }

    suspend fun updateDefaultSplitMode(mode: SplitMode) = set(PreferenceKeys.DEFAULT_SPLIT_MODE, mode.name)

    suspend fun updateFillCategoryFromPayee(enabled: Boolean) = set(PreferenceKeys.FILL_CATEGORY_FROM_PAYEE, enabled)

    suspend fun updateAppLock(enabled: Boolean, graceSeconds: Int) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.APP_LOCK_ENABLED] = enabled
            prefs[PreferenceKeys.APP_LOCK_GRACE_SECONDS] = graceSeconds.coerceAtLeast(0)
        }
    }

    suspend fun updateHideInRecents(enabled: Boolean) = set(PreferenceKeys.HIDE_IN_RECENTS, enabled)

    /** Back to defaults. Used by delete-all. */
    suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences -> preferences[key] = value }
    }

    private inline fun <reified E : Enum<E>> enumOr(raw: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == raw } ?: default

    companion object {
        const val DEFAULT_NOTIFICATIONS_ENABLED = true
        const val DEFAULT_CURRENCY = "INR"
        const val DEFAULT_HAPTICS_ENABLED = true
        const val DEFAULT_REPORTS_ENABLED = true
        const val DEFAULT_LABS_ENABLED = false
    }

    private object PreferenceKeys {
        val TRANSITION_DURATION_MILLIS = intPreferencesKey("transition_duration_millis")
        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
        // Key kept from the earlier string setting, so existing installs keep their choice.
        val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val PALETTE = stringPreferencesKey("palette")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val REPORTS_ENABLED = booleanPreferencesKey("reports_enabled")
        val LABS_ENABLED = booleanPreferencesKey("labs_enabled")
        val NUMBER_FORMAT = stringPreferencesKey("number_format")
        val WEEK_START = stringPreferencesKey("week_start")
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_MINUTES = intPreferencesKey("daily_reminder_minutes")
        val SETTLE_UP_NUDGES = booleanPreferencesKey("settle_up_nudges")
        val DEFAULT_ACCOUNT_ID = longPreferencesKey("default_account_id")
        val DEFAULT_SPLIT_MODE = stringPreferencesKey("default_split_mode")
        val FILL_CATEGORY_FROM_PAYEE = booleanPreferencesKey("fill_category_from_payee")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_GRACE_SECONDS = intPreferencesKey("app_lock_grace_seconds")
        val HIDE_IN_RECENTS = booleanPreferencesKey("hide_in_recents")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val UPI_ID = stringPreferencesKey("upi_id")
    }
}
