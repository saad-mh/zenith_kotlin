package com.saadm.zenith.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.saadm.zenith.domain.transaction.dayLabel
import com.saadm.zenith.ui.theme.ZenithSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Today, Yesterday or a picked date. Nothing later than [today] can be chosen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateChips(date: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val yesterday = today.minusDays(1)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(ZenithSpacing.space2)) {
        FilterChip("Today", date == today, { onPick(today) })
        FilterChip("Yesterday", date == yesterday, { onPick(yesterday) })
        val other = date != today && date != yesterday
        FilterChip(if (other) dayLabel(date, today) else "Pick date", other, { picking = true })
    }
    if (picking) {
        PastDatePicker(
            initial = date,
            today = today,
            onPick = {
                onPick(it)
                picking = false
            },
            onDismiss = { picking = false }
        )
    }
}

private fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastDatePicker(initial: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // The picker works in UTC days.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.utcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= today.utcMillis()
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis == null) onDismiss()
                else onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text("Use date") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}
