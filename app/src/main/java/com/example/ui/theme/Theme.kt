package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val MaaryaDarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = VelvetObsidian,
    primaryContainer = ChampagneGoldDark,
    onPrimaryContainer = ChampagneGoldBright,
    secondary = RoseSilk,
    onSecondary = VelvetObsidian,
    secondaryContainer = RoseSilkContainer,
    onSecondaryContainer = PearlIvory,
    tertiary = DiamondIce,
    onTertiary = VelvetObsidian,
    background = VelvetObsidian,
    onBackground = PearlIvory,
    surface = VelvetSurface,
    onSurface = PearlIvory,
    surfaceVariant = VelvetCard,
    onSurfaceVariant = WarmTaupe,
    surfaceContainerLowest = VelvetObsidian,
    surfaceContainerLow = VelvetSurface,
    surfaceContainer = VelvetCard,
    surfaceContainerHigh = VelvetCardElevated,
    outline = VelvetOutline,
    outlineVariant = Color(0xFF2D2231),
    error = CrimsonAlert,
    onError = VelvetObsidian
)

private val MaaryaLightColorScheme = lightColorScheme(
    primary = BronzeGold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6E8),
    onPrimaryContainer = Color(0xFF3D0620),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFF120B18),
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = EspressoInk,
    surface = IvorySurface,
    onSurface = EspressoInk,
    surfaceVariant = IvoryCard,
    onSurfaceVariant = MutedPlum,
    outline = Color(0xFFE2CEE8),
    error = Color(0xFFD91E5B),
    onError = Color.White
)

private val MaaryaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MaaryaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MaaryaDarkColorScheme else MaaryaLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaaryaTypography,
        shapes = MaaryaShapes,
        content = content
    )
}
