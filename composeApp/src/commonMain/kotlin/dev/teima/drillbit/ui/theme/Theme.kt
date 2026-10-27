package dev.teima.drillbit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.teima.drillbit.core.dotbook.StrideDifficulty
import dev.teima.drillbit.core.model.Section

/** Night-rehearsal palette: dark turf, stadium-light white, Kotlin purple as the accent. */
object DrillbitColors {
    val Background = Color(0xFF0B140E)
    val Surface = Color(0xFF13201A)
    val SurfaceHigh = Color(0xFF1B2B23)
    val TurfLight = Color(0xFF2F6B3A)
    val TurfDark = Color(0xFF285C32)
    val Chalk = Color(0xFFF2F5EE)
    val ChalkFaint = Color(0x66F2F5EE)
    val Accent = Color(0xFFB69CFF)
    val AccentStrong = Color(0xFF7F52FF)
    val Danger = Color(0xFFFF5370)
    val Warning = Color(0xFFFFC857)
    val Safe = Color(0xFF6EE7A8)
    val Muted = Color(0xFF9DB3A6)

    fun section(section: Section): Color =
        when (section) {
            Section.WOODWIND -> Color(0xFF6FD3FF)
            Section.BRASS -> Color(0xFFFFD45C)
            Section.LOW_BRASS -> Color(0xFFFF9466)
            Section.PERCUSSION -> Color(0xFFEDEDED)
            Section.GUARD -> Color(0xFFFF7EB6)
        }

    fun stride(difficulty: StrideDifficulty): Color =
        when (difficulty) {
            StrideDifficulty.HOLD -> Muted
            StrideDifficulty.COMFORTABLE -> Safe
            StrideDifficulty.LARGE -> Warning
            StrideDifficulty.TOO_LARGE -> Danger
        }
}

private val scheme =
    darkColorScheme(
        primary = DrillbitColors.Accent,
        onPrimary = Color(0xFF1C0B4D),
        primaryContainer = DrillbitColors.AccentStrong,
        onPrimaryContainer = Color.White,
        secondary = DrillbitColors.Safe,
        background = DrillbitColors.Background,
        onBackground = DrillbitColors.Chalk,
        surface = DrillbitColors.Surface,
        onSurface = DrillbitColors.Chalk,
        surfaceVariant = DrillbitColors.SurfaceHigh,
        onSurfaceVariant = DrillbitColors.Muted,
        surfaceContainer = DrillbitColors.Surface,
        surfaceContainerHigh = DrillbitColors.SurfaceHigh,
        surfaceContainerHighest = DrillbitColors.SurfaceHigh,
        outline = Color(0xFF3A5246),
        error = DrillbitColors.Danger,
    )

@Composable
fun DrillbitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
