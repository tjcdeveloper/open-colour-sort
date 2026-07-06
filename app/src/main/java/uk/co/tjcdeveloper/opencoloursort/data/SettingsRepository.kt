package uk.co.tjcdeveloper.opencoloursort.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val colorblindSymbols = booleanPreferencesKey("colorblind_symbols")
        val haptics = booleanPreferencesKey("haptics")
        val palette = stringPreferencesKey("palette")
        val tubeBottomRadius = intPreferencesKey("tube_bottom_radius")
    }

    val settings: Flow<Settings> = context.settingsStore.data
        // A corrupted store falls back to defaults instead of crashing.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
        Settings(
            theme = prefs[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            colorblindSymbols = prefs[Keys.colorblindSymbols] ?: false,
            haptics = prefs[Keys.haptics] ?: true,
            palette = prefs[Keys.palette]?.let { runCatching { PaletteMode.valueOf(it) }.getOrNull() }
                ?: PaletteMode.VIVID,
            tubeBottomRadius = (prefs[Keys.tubeBottomRadius] ?: 24)
                .coerceIn(Settings.MIN_TUBE_RADIUS, Settings.MAX_TUBE_RADIUS),
        )
    }

    suspend fun setTheme(mode: ThemeMode) =
        context.settingsStore.edit { it[Keys.theme] = mode.name }

    suspend fun setColorblindSymbols(enabled: Boolean) =
        context.settingsStore.edit { it[Keys.colorblindSymbols] = enabled }

    suspend fun setHaptics(enabled: Boolean) =
        context.settingsStore.edit { it[Keys.haptics] = enabled }

    suspend fun setPalette(mode: PaletteMode) =
        context.settingsStore.edit { it[Keys.palette] = mode.name }

    suspend fun setTubeBottomRadius(radius: Int) =
        context.settingsStore.edit {
            it[Keys.tubeBottomRadius] =
                radius.coerceIn(Settings.MIN_TUBE_RADIUS, Settings.MAX_TUBE_RADIUS)
        }
}
