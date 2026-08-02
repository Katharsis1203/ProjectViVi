package com.example.visualvocab.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme =
    darkColorScheme(
        primary = ElectricBlue,
        onPrimary = PureWhite,
        primaryContainer = ElectricBlueContainer,
        onPrimaryContainer = PureWhite,

        secondary = ElectricBlueSoft,
        onSecondary = InkBlack,
        secondaryContainer = CarbonRaised,
        onSecondaryContainer = PureWhite,

        tertiary = VioletAccent,
        onTertiary = InkBlack,
        tertiaryContainer = VioletContainer,
        onTertiaryContainer = PureWhite,

        background = InkBlack,
        onBackground = PureWhite,

        surface = Carbon,
        onSurface = PureWhite,
        surfaceVariant = CarbonRaised,
        onSurfaceVariant = SoftWhite,

        outline = DarkOutline,
        outlineVariant =
            DarkOutline.copy(alpha = 0.72f),

        error = ErrorRed,
        onError = InkBlack,

        scrim = InkBlack
    )

@Composable
fun VisualVocabTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window =
            (view.context as Activity).window

        window.statusBarColor =
            AppColorScheme.background.toArgb()

        window.navigationBarColor =
            AppColorScheme.background.toArgb()

        WindowCompat
            .getInsetsController(
                window,
                view
            )
            .isAppearanceLightStatusBars = false

        WindowCompat
            .getInsetsController(
                window,
                view
            )
            .isAppearanceLightNavigationBars = false
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}