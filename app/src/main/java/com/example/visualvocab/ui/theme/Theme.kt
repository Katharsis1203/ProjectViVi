package com.example.visualvocab.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightAppColorScheme = lightColorScheme(
    primary = ExplorerBlue,
    onPrimary = PureWhite,
    primaryContainer = ExplorerBlueSoft,
    onPrimaryContainer = ExplorerBlueDark,

    secondary = DiscoveryMint,
    onSecondary = NightInk,
    secondaryContainer = DiscoveryMintSoft,
    onSecondaryContainer = Ink,

    tertiary = CreatorPurple,
    onTertiary = PureWhite,
    tertiaryContainer = CreatorPurpleSoft,
    onTertiaryContainer = Ink,

    background = CloudWhite,
    onBackground = Ink,

    surface = PureWhite,
    onSurface = Ink,
    surfaceVariant = ColorTokens.SurfaceVariant,
    onSurfaceVariant = Slate,

    outline = PaleOutline,
    outlineVariant = PaleOutline.copy(alpha = 0.72f),

    error = FriendlyCoral,
    onError = PureWhite,
    errorContainer = FriendlyCoralSoft,
    onErrorContainer = ColorTokens.ErrorInk,

    scrim = NightInk
)

private val DarkAppColorScheme = darkColorScheme(
    primary = ColorTokens.DarkPrimary,
    onPrimary = NightInk,
    primaryContainer = ExplorerBlueDark,
    onPrimaryContainer = NightText,

    secondary = DiscoveryMint,
    onSecondary = NightInk,
    secondaryContainer = ColorTokens.DarkMintContainer,
    onSecondaryContainer = NightText,

    tertiary = ColorTokens.DarkTertiary,
    onTertiary = NightInk,
    tertiaryContainer = ColorTokens.DarkTertiaryContainer,
    onTertiaryContainer = NightText,

    background = NightInk,
    onBackground = NightText,

    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceRaised,
    onSurfaceVariant = NightMuted,

    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.72f),

    error = ColorTokens.DarkError,
    onError = NightInk,
    errorContainer = ColorTokens.DarkErrorContainer,
    onErrorContainer = NightText,

    scrim = NightInk
)

private object ColorTokens {
    val SurfaceVariant = androidx.compose.ui.graphics.Color(0xFFF0F2F8)
    val ErrorInk = androidx.compose.ui.graphics.Color(0xFF7B1E1E)
    val DarkPrimary = androidx.compose.ui.graphics.Color(0xFFAFC0FF)
    val DarkMintContainer = androidx.compose.ui.graphics.Color(0xFF174B3E)
    val DarkTertiary = androidx.compose.ui.graphics.Color(0xFFCDBDFF)
    val DarkTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF44377A)
    val DarkError = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
    val DarkErrorContainer = androidx.compose.ui.graphics.Color(0xFF7A2930)
}

@Composable
fun VisualVocabTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val colorScheme = if (darkTheme) DarkAppColorScheme else LightAppColorScheme

    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()

        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
