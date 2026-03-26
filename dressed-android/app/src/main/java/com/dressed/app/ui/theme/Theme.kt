package com.dressed.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = WardrobeTopBarPlum,
    onPrimary = WardrobeOnBarText,
    primaryContainer = WardrobeSurfaceMuted,
    onPrimaryContainer = WardrobeInk,
    secondary = WardrobeViolet,
    onSecondary = WardrobeOnBarText,
    tertiary = WardrobeVioletBright,
    onTertiary = WardrobeSurfaceCard,
    secondaryContainer = WardrobeSurfaceMuted,
    onSecondaryContainer = WardrobeDeepPurple,
    background = WardrobeScreenLavender,
    onBackground = WardrobeInk,
    surface = WardrobeSurfaceCard,
    onSurface = WardrobeInk,
    surfaceVariant = WardrobeSurfaceMuted,
    onSurfaceVariant = WardrobeTextMuted,
    outline = WardrobeTopBarPlum.copy(alpha = 0.22f),
)

private val DarkColors = darkColorScheme(
    primary = WardrobeViolet,
    onPrimary = WardrobeOnBarText,
    primaryContainer = WardrobeTopBarPlum,
    onPrimaryContainer = WardrobeOnBarText,
    secondary = WardrobeVioletBright,
    onSecondary = WardrobeOnBarText,
    tertiary = WardrobeTan,
    onTertiary = WardrobeInk,
    background = WardrobeInk,
    onBackground = WardrobeScreenLavender,
    surface = WardrobeDeepPurple,
    onSurface = WardrobeScreenLavender,
    surfaceVariant = WardrobeTopBarPlum,
    onSurfaceVariant = WardrobeTextMuted.copy(alpha = 0.9f),
    outline = WardrobeOnBarText.copy(alpha = 0.2f),
)

@Composable
fun DressedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Off by default so the app matches the designed palette (see dressed-mockup.html). */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
