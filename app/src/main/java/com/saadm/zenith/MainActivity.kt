package com.saadm.zenith

import java.time.Instant
import java.time.Duration
import androidx.compose.runtime.SideEffect
import android.view.ViewTreeObserver
import android.view.View
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
        var ready = false
        holdSplashUntil { ready }
        fadeSplashOut()
        setContent {
            val appPreferencesStore = remember { AppPreferencesStore(applicationContext) }
            val storedPreferences by appPreferencesStore.preferencesFlow.collectAsState(initial = null)
            // Draw nothing until the stored theme is known, so the first frame is never in the wrong theme.
            val appPreferences = storedPreferences ?: return@setContent
            SideEffect { ready = true }
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

    // Keeps the system splash up instead of showing a blank frame while the theme is read.
    private fun holdSplashUntil(ready: () -> Boolean) {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (!ready()) return false
                content.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }

    // Lets the rings finish, then grows and fades the splash into the first screen.
    private fun fadeSplashOut() {
        splashScreen.setOnExitAnimationListener { splash ->
            val started = splash.iconAnimationStart
            val length = splash.iconAnimationDuration
            val remaining = if (started != null && length != null) {
                (length - Duration.between(started, Instant.now())).toMillis().coerceIn(0, length.toMillis())
            } else 0
            splash.iconView?.animate()?.scaleX(1.35f)?.scaleY(1.35f)?.setStartDelay(remaining)?.setDuration(SPLASH_EXIT_MILLIS)
            splash.animate().alpha(0f).setStartDelay(remaining).setDuration(SPLASH_EXIT_MILLIS)
                .withEndAction { splash.remove() }
        }
    }
}

private const val SPLASH_EXIT_MILLIS = 260L
