package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = OrangeAccent,
    secondary = TextSlate200,
    tertiary = OrangeAccentDark,
    background = DeepSlate900,
    surface = SolidSlate800,
    onPrimary = TextSlate50,
    onSecondary = DeepSlate900,
    onBackground = TextSlate50,
    onSurface = TextSlate200,
    outline = BorderSlate600
  )

private val LightColorScheme = DarkColorScheme // Keep consistent professional dark slate mode for both

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamicColor to preserve beautiful curated Slate & Orange branding
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
