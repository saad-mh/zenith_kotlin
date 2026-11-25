package com.saadm.zenith.data.preferences

import com.saadm.zenith.domain.model.MonthPeriod
import com.saadm.zenith.domain.model.MonthPeriods
import com.saadm.zenith.domain.model.NumberFormat
import com.saadm.zenith.domain.model.SplitMode
import com.saadm.zenith.domain.model.WeekStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * The app's settings as Flows, and the single place other layers read them from.
 * Writes go straight to the DataStore-backed [AppPreferencesStore].
 */
class SettingsRepository @Inject constructor(private val store: AppPreferencesStore) {

    val settings: Flow<AppPreferences> = store.preferencesFlow

    val monthStartDay: Flow<Int> = settings.map { it.monthStartDay }

    val numberFormat: Flow<NumberFormat> = settings.map { it.numberFormat }

    val weekStart: Flow<WeekStart> = settings.map { it.weekStart }

    /** The reporting month that contains [today], using the month start day from settings. */
    fun currentMonth(today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<MonthPeriod> =
        monthStartDay.map { day -> MonthPeriods.containing(today, day, zone) }

    suspend fun setMonthStartDay(day: Int) = store.updateMonthStartDay(day)

    suspend fun setNumberFormat(format: NumberFormat) = store.updateNumberFormat(format)

    suspend fun setWeekStart(start: WeekStart) = store.updateWeekStart(start)

    suspend fun setDailyReminder(enabled: Boolean, minutesAfterMidnight: Int) =
        store.updateDailyReminder(enabled, minutesAfterMidnight)

    suspend fun setSettleUpNudges(enabled: Boolean) = store.updateSettleUpNudges(enabled)

    suspend fun setDefaultAccountId(accountId: Long?) = store.updateDefaultAccountId(accountId)

    suspend fun setProfile(name: String, upiId: String) = store.updateProfile(name, upiId)

    suspend fun setDefaultSplitMode(mode: SplitMode) = store.updateDefaultSplitMode(mode)

    suspend fun setFillCategoryFromPayee(enabled: Boolean) = store.updateFillCategoryFromPayee(enabled)

    suspend fun setAppLock(enabled: Boolean, graceSeconds: Int) = store.updateAppLock(enabled, graceSeconds)

    suspend fun setHideInRecents(enabled: Boolean) = store.updateHideInRecents(enabled)
}
