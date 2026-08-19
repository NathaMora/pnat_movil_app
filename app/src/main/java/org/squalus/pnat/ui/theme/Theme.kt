package org.squalus.pnat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PnatLightColorScheme = lightColorScheme(
    primary = PnatBlue,
    onPrimary = PnatSurface,
    primaryContainer = PnatBlueContainer,
    onPrimaryContainer = PnatBlueDeep,
    secondary = PnatBlueDark,
    onSecondary = PnatSurface,
    secondaryContainer = PnatBlueContainerLight,
    onSecondaryContainer = PnatBlueDeep,
    tertiary = PnatBlueDeep,
    onTertiary = PnatSurface,
    background = PnatBackground,
    onBackground = PnatTextPrimary,
    surface = PnatSurface,
    onSurface = PnatTextPrimary,
    surfaceVariant = PnatBackground,
    onSurfaceVariant = PnatTextBody,
    outline = PnatBorder,
    outlineVariant = PnatBlueContainer,
    error = PnatError,
    onError = PnatSurface,
)

@Composable
fun Pnat_mobileTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PnatLightColorScheme,
        typography = Typography,
        shapes = PnatShapes,
        content = content
    )
}
