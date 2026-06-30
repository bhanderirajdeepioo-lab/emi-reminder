package com.emireminder.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = Indigo600,
    onPrimary          = Color.White,
    primaryContainer   = Indigo100,
    onPrimaryContainer = Violet900,
    secondary            = Violet600,
    onSecondary          = Color.White,
    secondaryContainer   = Violet100,
    onSecondaryContainer = Violet900,
    tertiary             = WarnOrange,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFFFF3E0),
    onTertiaryContainer  = WarnOrange,
    background    = Indigo50,
    onBackground  = Color(0xFF1E1B4B),
    surface       = Color.White,
    onSurface     = Color(0xFF1E1B4B),
    surfaceVariant    = Indigo100,
    onSurfaceVariant  = Gray500,
    outline = Gray200,
    error   = UrgentRed,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary            = Violet100,
    onPrimary          = Violet900,
    primaryContainer   = Violet600,
    onPrimaryContainer = Color.White,
    secondary            = Cyan400,
    onSecondary          = Cyan700,
    tertiary             = Amber400,
    onTertiary           = Amber700,
    background    = DarkBg,
    onBackground  = Color(0xFFE8E6F0),
    surface       = DarkCard,
    onSurface     = Color(0xFFE8E6F0),
    surfaceVariant    = DarkLine,
    onSurfaceVariant  = Color(0xFFB0AEC0),
    outline = DarkLine,
    error   = Color(0xFFFF6B6B),
    onError = Color.White,
)

@Composable
fun EmiReminderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
