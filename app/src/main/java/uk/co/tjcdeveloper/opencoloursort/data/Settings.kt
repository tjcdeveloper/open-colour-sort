package uk.co.tjcdeveloper.opencoloursort.data

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class PaletteMode { VIVID, SOFT }

/**
 * User settings, per the handoff's State Management section:
 * theme (light/dark/system), colorblindSymbols, haptics, palette
 * (vivid/soft), tubeBottomRadius (4-28dp).
 */
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val colorblindSymbols: Boolean = false,
    val haptics: Boolean = true,
    val palette: PaletteMode = PaletteMode.VIVID,
    val tubeBottomRadius: Int = 24,
) {
    companion object {
        const val MIN_TUBE_RADIUS = 4
        const val MAX_TUBE_RADIUS = 28
    }
}
