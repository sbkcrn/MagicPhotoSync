package com.rabbit.magicphotos.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Theme Palette
val Black900 = Color(0xFF000000)      // Pure black - background
val Black800 = Color(0xFF080808)      // Near black - cards
val Black700 = Color(0xFF121212)      // Dark gray - elevated surfaces
val Black600 = Color(0xFF1E1E1E)      // Medium dark - borders
val Black500 = Color(0xFF2A2A2A)      // Lighter dark

// Legacy beige names mapped to dark theme (for compatibility)
val Beige50 = Color(0xFF0A0A0A)
val Beige100 = Color(0xFF121212)
val Beige200 = Color(0xFF1E1E1E)
val Beige300 = Color(0xFF2A2A2A)
val Beige400 = Color(0xFF3A3A3A)
val Beige500 = Color(0xFF4A4A4A)

// Accent Colors - Leuchtorange #FF4D06
val RabbitOrange = Color(0xFFFF4D06)        // Primary accent - Leuchtorange
val RabbitOrangeDark = Color(0xFFE64400)    // Pressed state
val RabbitOrangeLight = Color(0xFFFF6B2C)   // Lighter variant

// Text Colors (for dark theme)
val Charcoal = Color(0xFFFFFFFF)            // Primary text (white)
val CharcoalLight = Color(0xFFE0E0E0)       // Slightly muted white
val CharcoalMuted = Color(0xFF888888)       // Secondary text
val OffWhite = Color(0xFFFAFAFA)            // Light text

// Status Colors
val Success = Color(0xFF4CAF50)
val Error = Color(0xFFE53935)
val Warning = Color(0xFFFF9800)

// Surface Colors for Dark Theme
val SurfaceLight = Black800
val SurfaceVariantLight = Black700
val BackgroundLight = Black900
val OnSurfaceLight = Charcoal
val OnSurfaceVariantLight = CharcoalLight
