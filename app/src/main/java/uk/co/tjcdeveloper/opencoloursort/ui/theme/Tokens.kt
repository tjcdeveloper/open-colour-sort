package uk.co.tjcdeveloper.opencoloursort.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design tokens from the handoff. Dark is the primary theme (true-black
 * window); light mirrors the Open Twenty Forty-Eight brand neutrals.
 */
data class ColourScheme(
    val isDark: Boolean,
    val window: Color,
    val card: Color,        // settings/pack cards, dialogs
    val chip: Color,        // stat chips, level chips, progress track
    val raised: Color,      // segmented control track, toggle off
    val border: Color,      // tube borders, dialog hairline, dashed locked
    val borderSoft: Color,  // zen outline / scrollbar (dark only differs)
    val textMuted: Color,
    val textPrimary: Color,
    val textBright: Color,
    val chipLabel: Color,   // stat chip label colour
    val chipValue: Color,   // stat chip value colour
    val iconButtonBg: Color,
    val iconButtonFg: Color,
    val lockedText: Color,
)

val DarkScheme = ColourScheme(
    isDark = true,
    window = Color(0xFF000000),
    card = Color(0xFF16120E),
    chip = Color(0xFF1C1712),
    raised = Color(0xFF241F18),
    border = Color(0xFF2A241D),
    borderSoft = Color(0xFF3A342C),
    textMuted = Color(0xFF8D8579),
    textPrimary = Color(0xFFD8CFC2),
    textBright = Color(0xFFF5EFE6),
    chipLabel = Color(0xFF8D8579),
    chipValue = Color(0xFFF5EFE6),
    iconButtonBg = Color(0xFF2A241D),
    iconButtonFg = Color(0xFF8D8579),
    lockedText = Color(0xFF4A443C),
)

val LightScheme = ColourScheme(
    isDark = false,
    window = Color(0xFFFAF8EF),
    card = Color(0xFFF2ECE1),
    chip = Color(0xFFBBADA0),
    raised = Color(0xFFEEE4DA),
    border = Color(0xFFBBADA0),
    borderSoft = Color(0xFFD6C9BB),
    textMuted = Color(0xFFA1937F),
    textPrimary = Color(0xFF776E65),
    textBright = Color(0xFFFFFFFF),
    chipLabel = Color(0xFFEEE4DA),
    chipValue = Color(0xFFFFFFFF),
    iconButtonBg = Color(0xFFD6C9BB),
    iconButtonFg = Color(0xFFA99A89),
    lockedText = Color(0xFFBCAE9D),
)

/** Brand accent colours, identical in both themes. */
object Accent {
    val primary = Color(0xFF8F7A66)
    val onPrimary = Color(0xFFF9F6F2)
    val link = Color(0xFFC4A183)
}

val LocalScheme = staticCompositionLocalOf { DarkScheme }
