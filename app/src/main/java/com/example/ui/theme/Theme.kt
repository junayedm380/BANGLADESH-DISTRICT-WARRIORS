package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DistrictWarriorsColorScheme = darkColorScheme(
    primary = BengalEmeraldBright,
    onPrimary = ObsidianBg,
    primaryContainer = BengalEmeraldContainer,
    onPrimaryContainer = BengalEmeraldBright,
    secondary = DhakaAmber,
    onSecondary = ObsidianBg,
    secondaryContainer = DhakaAmberContainer,
    onSecondaryContainer = DhakaAmber,
    tertiary = TacticalCyan,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = TacticalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = TacticalSurfaceBorder,
    error = CrimsonStrike,
    onError = TextPrimary,
    errorContainer = CrimsonContainer,
    onErrorContainer = TextPrimary
)

val TacticalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun DistrictWarriorsTheme(
    isBengali: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DistrictWarriorsColorScheme,
        typography = buildGameTypography(isBengali),
        shapes = TacticalShapes,
        content = content
    )
}
