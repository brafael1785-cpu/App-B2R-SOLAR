package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhite,
    primaryContainer = MintContainer,
    onPrimaryContainer = ForestGreen,
    secondary = VibrantGreen,
    onSecondary = PureWhite,
    secondaryContainer = SolarGoldSoft,
    onSecondaryContainer = InkPrimary,
    tertiary = ForestGreen,
    onTertiary = PureWhite,
    background = SageBackground,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = MintContainer,
    onSurfaceVariant = InkSecondary,
    outline = SoftBorder,
    error = ErrorRed,
    onError = PureWhite
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = LeafAccent,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextSecondary,
    tertiary = ForestGreen,
    onTertiary = PureWhite,
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = ColorTokens.DarkOutline,
    error = ErrorRed,
    onError = PureWhite
)

private object ColorTokens {
    val DarkOutline = androidx.compose.ui.graphics.Color(0xFF254E39)
}

val B2RShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun B2RSolarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = B2RTypography,
        shapes = B2RShapes,
        content = content
    )
}
