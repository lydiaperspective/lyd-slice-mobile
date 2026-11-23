package com.lydiaperspective.lydslice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SliceColors = darkColorScheme(
    primary = Color(0xFFB8A1FF),
    secondary = Color(0xFF9DD9C7),
    tertiary = Color(0xFFFFC98B),
    background = Color(0xFF111116),
    surface = Color(0xFF1A1A22),
    surfaceVariant = Color(0xFF292934)
)

@Composable
fun LydSliceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SliceColors,
        content = content
    )
}
