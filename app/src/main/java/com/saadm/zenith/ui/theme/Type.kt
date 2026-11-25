@file:OptIn(ExperimentalTextApi::class)

package com.saadm.zenith.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.saadm.zenith.R

// Variable fonts: one file per family, with the weight set on the wght axis (API 26+).
val FrauncesFamily = FontFamily(
    Font(R.font.fraunces_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.fraunces_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

val WorkSansFamily = FontFamily(
    Font(R.font.work_sans_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.work_sans_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.work_sans_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

/** Named styles from the design canvas. Caption is drawn in ink-muted at the call site. */
data class ZenithTypeScale(
    val display: TextStyle,
    val heading: TextStyle,
    val subheading: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val numeric: TextStyle,
    val caption: TextStyle,
)

val ZenithType = ZenithTypeScale(
    display = TextStyle(
        fontFamily = FrauncesFamily, fontWeight = FontWeight.Medium,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.4).sp,
        fontFeatureSettings = "lnum",
    ),
    heading = TextStyle(
        fontFamily = FrauncesFamily, fontWeight = FontWeight.Medium,
        fontSize = 24.sp, lineHeight = 30.sp,
        fontFeatureSettings = "lnum",
    ),
    subheading = TextStyle(
        fontFamily = WorkSansFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 26.sp,
    ),
    body = TextStyle(
        fontFamily = WorkSansFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    bodyStrong = TextStyle(
        fontFamily = WorkSansFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    numeric = TextStyle(
        fontFamily = WorkSansFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 22.sp,
        fontFeatureSettings = "tnum, lnum",
    ),
    caption = TextStyle(
        fontFamily = WorkSansFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
)

// Material 3 roles mapped onto the Zenith scale so existing stock components pick it up.
val Typography = Typography(
    displayLarge = ZenithType.display,
    displayMedium = ZenithType.display,
    displaySmall = ZenithType.display,
    headlineLarge = ZenithType.heading,
    headlineMedium = ZenithType.heading,
    headlineSmall = ZenithType.heading,
    titleLarge = ZenithType.heading,
    titleMedium = ZenithType.subheading,
    titleSmall = ZenithType.bodyStrong,
    bodyLarge = ZenithType.body,
    bodyMedium = ZenithType.body,
    bodySmall = ZenithType.caption,
    labelLarge = ZenithType.bodyStrong,
    labelMedium = ZenithType.caption,
    labelSmall = ZenithType.caption,
)
