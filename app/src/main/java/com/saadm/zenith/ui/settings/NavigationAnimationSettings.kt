package com.saadm.zenith.ui.settings

enum class TransitionStyle(
    val label: String,
    val storageValue: String
) {
    SlideOnly(
        label = "Full-width slide",
        storageValue = "slide_only"
    ),
    FadeSlide(
        label = "Fade-Slide Blend",
        storageValue = "fade_slide"
    );

    companion object {
        fun fromStorageValue(value: String?): TransitionStyle {
            return entries.firstOrNull { it.storageValue == value } ?: DEFAULT_TRANSITION_STYLE
        }
    }
}

// Long enough that the animation is still visible after the incoming screen has been composed.
// A stored value from the older, shorter set falls back to the default.
val TransitionDurationOptions = listOf(200, 300, 400)
const val DEFAULT_TRANSITION_DURATION_MILLIS = 300
val DEFAULT_TRANSITION_STYLE = TransitionStyle.SlideOnly

fun normalizeTransitionDurationMillis(durationMillis: Int): Int {
    return TransitionDurationOptions.firstOrNull { it == durationMillis }
        ?: DEFAULT_TRANSITION_DURATION_MILLIS
}
