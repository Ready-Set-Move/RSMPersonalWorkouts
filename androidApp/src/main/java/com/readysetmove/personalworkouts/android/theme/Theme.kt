package com.readysetmove.personalworkouts.android.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColors = lightColors(
    primary = rsmLightGreen,
    onPrimary = rsmLightGrey,
    surface = Color.White,
    background = rsmLightGrey,
)

private val DarkColors = darkColors(
    primary = rsmLightGreen,
    onPrimary = rsmLightGrey,
    onSurface = rsmMediumGrey,
    background = Color.Black,
)

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalSpacings provides Spacings()) {
        MaterialTheme(colors = colors,
            typography = Typography(defaultFontFamily = SourceSansPro),
            content = content)
    }
}

object AppTheme {
    /**
     * Proxy to [MaterialTheme]
     */
    val colors: Colors
        @Composable
        get() = MaterialTheme.colors

    /**
     * Proxy to [MaterialTheme]
     */
    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    /**
     * Proxy to [MaterialTheme]
     */
    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes

    /**
     * Retrieves the current [Spacings] at the call site's position in the hierarchy.
     */
    val spacings: Spacings
        @Composable
        get() = LocalSpacings.current
}