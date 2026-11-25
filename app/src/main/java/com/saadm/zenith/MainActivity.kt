package com.saadm.zenith

import com.saadm.zenith.ui.theme.zenithColors
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.SystemBarStyle
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.saadm.zenith.data.preferences.AppPreferencesStore
import com.saadm.zenith.ui.navigation.ZenithApp
import com.saadm.zenith.ui.settings.normalizeTransitionDurationMillis
import com.saadm.zenith.ui.theme.ZenithTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appPreferencesStore = remember { AppPreferencesStore(applicationContext) }
            val storedPreferences by appPreferencesStore.preferencesFlow.collectAsState(initial = null)
            // Draw nothing until the stored theme is known, so the first frame is never in the wrong theme.
            val appPreferences = storedPreferences ?: return@setContent
            ZenithTheme(mode = appPreferences.themeMode, palette = appPreferences.palette) {
                // Status and navigation bar icons follow the app theme, which can differ from the system's.
                val dark = zenithColors().isDark
                LaunchedEffect(dark) {
                    val bars = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }
                ZenithApp(
                    transitionDurationMillis = normalizeTransitionDurationMillis(
                        appPreferences.transitionDurationMillis
                    ),
                    transitionStyle = appPreferences.transitionStyle
                )
            }
        }
    }
}
