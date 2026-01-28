package com.op1sync.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// OP-1 Sync uses dark theme only (following TE aesthetic)
private val OP1SyncColorScheme = darkColorScheme(
    // Primary colors (Orange accent)
    primary = TeOrange,
    onPrimary = TeWhite,
    primaryContainer = TeOrangeDark,
    onPrimaryContainer = TeWhite,
    
    // Secondary colors
    secondary = TeMediumGray,
    onSecondary = TeLightGray,
    secondaryContainer = TeDarkGray,
    onSecondaryContainer = TeLightGray,
    
    // Tertiary (alternative accent)
    tertiary = TeGreen,
    onTertiary = TeWhite,
    
    // Background & Surface
    background = TeBlack,
    onBackground = TeLightGray,
    surface = TeBlack,
    onSurface = TeLightGray,
    surfaceVariant = TeDarkGray,
    onSurfaceVariant = TeLightGray,
    
    // Error
    error = TeRed,
    onError = TeWhite,
    errorContainer = TeRed,
    onErrorContainer = TeWhite,
    
    // Outline
    outline = TeMediumGray,
    outlineVariant = TeDarkGray,
    
    // Inverse
    inverseSurface = TeLightGray,
    inverseOnSurface = TeBlack,
    inversePrimary = TeOrangeDark
)

@Composable
fun OP1SyncTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = OP1SyncColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TeBlack.toArgb()
            window.navigationBarColor = TeBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
