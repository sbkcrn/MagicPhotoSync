package com.rabbit.magicphotos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    // Primary - Leuchtorange
    primary = RabbitOrange,
    onPrimary = Black900,
    primaryContainer = RabbitOrangeDark,
    onPrimaryContainer = OffWhite,
    
    // Secondary - Dark grays
    secondary = Black500,
    onSecondary = OffWhite,
    secondaryContainer = Black600,
    onSecondaryContainer = OffWhite,
    
    // Tertiary - Muted
    tertiary = CharcoalMuted,
    onTertiary = Black900,
    tertiaryContainer = Black600,
    onTertiaryContainer = OffWhite,
    
    // Background & Surface - Black
    background = Black900,
    onBackground = OffWhite,
    surface = Black800,
    onSurface = OffWhite,
    surfaceVariant = Black700,
    onSurfaceVariant = CharcoalLight,
    
    // Inverse
    inverseSurface = OffWhite,
    inverseOnSurface = Black900,
    inversePrimary = RabbitOrangeDark,
    
    // Outline
    outline = Black500,
    outlineVariant = Black600,
    
    // Error
    error = Error,
    onError = Black900,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Scrim
    scrim = Black900.copy(alpha = 0.7f),
)

@Composable
fun MagicPhotoSyncTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Black900.toArgb()
            window.navigationBarColor = Black900.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
