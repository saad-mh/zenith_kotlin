package com.saadm.zenith.ui.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saadm.zenith.data.db.AppDatabase
import com.saadm.zenith.ui.components.Lu
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DebugSeedViewModel @Inject constructor(private val database: AppDatabase) : ViewModel() {
    fun seed(onDone: (Int) -> Unit) {
        viewModelScope.launch { onDone(seedRandomData(database, LocalDate.now())) }
    }
}

/** Only in debug builds: adds random entries on top of whatever is there. */
@Composable
fun DebugTools(viewModel: DebugSeedViewModel = hiltViewModel()) {
    val context = LocalContext.current
    SettingsGroup("Debug") {
        SettingsRow(Lu.lucide_ic_sparkles, Tile.Plum, "Add random test data", onClick = {
            viewModel.seed { count -> Toast.makeText(context, "Added $count entries", Toast.LENGTH_SHORT).show() }
        })
    }
}
