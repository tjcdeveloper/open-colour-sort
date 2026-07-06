package uk.co.tjcdeveloper.opencoloursort.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.progressStore by preferencesDataStore(
    name = "progress",
    produceMigrations = { listOf(PositionalKeyMigration) },
)

/**
 * Per-level progress: best (lowest) move count for each solved level.
 * A level is solved iff it has a best-move entry. Keys are
 * "p_<packSlug>_l<level>", using the pack's stable slug so that packs can
 * be inserted or reordered without re-pointing existing saves.
 */
data class Progress(val bestMoves: Map<String, Int>) {
    fun isSolved(packSlug: String, level: Int): Boolean = key(packSlug, level) in bestMoves
    fun bestFor(packSlug: String, level: Int): Int? = bestMoves[key(packSlug, level)]
    fun solvedInPack(packSlug: String): Int =
        bestMoves.keys.count { it.startsWith("p_${packSlug}_l") }

    companion object {
        const val KEY_PREFIX = "p_"
        fun key(packSlug: String, level: Int) = "p_${packSlug}_l$level"
    }
}

/** Progress around a recorded solve, for one-shot unlock announcements. */
data class SolveResult(val before: Progress, val after: Progress)

/**
 * Schema v1 keyed levels on positional pack ids ("p<index>_l<level>").
 * This migration re-keys them onto the slugs of the packs that held those
 * positions at v1 (frozen history - do not update this list when packs
 * change; bump the schema instead) and stamps the schema version.
 */
internal object PositionalKeyMigration : DataMigration<Preferences> {
    val schemaKey: Preferences.Key<Int> = intPreferencesKey("schema_version")
    private const val CURRENT_SCHEMA = 2
    private val legacyKey = Regex("""p(\d+)_l(\d+)""")
    private val v1PackSlugs = listOf(
        "beginner", "easy-1", "easy-2", "intermediate-1", "intermediate-2",
        "hard-1", "hard-2", "hard-3", "extreme-1", "extreme-2", "final-challenge",
    )

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        (currentData[schemaKey] ?: 1) < CURRENT_SCHEMA

    override suspend fun migrate(currentData: Preferences): Preferences {
        val updated = currentData.toMutablePreferences()
        for ((key, value) in currentData.asMap()) {
            val match = legacyKey.matchEntire(key.name) ?: continue
            val moves = value as? Int ?: continue
            updated.remove(key)
            // toIntOrNull: a hand-mangled key must never crash the migration.
            val packIndex = match.groupValues[1].toIntOrNull() ?: continue
            val level = match.groupValues[2].toIntOrNull() ?: continue
            val slug = v1PackSlugs.getOrNull(packIndex) ?: continue
            updated[intPreferencesKey(Progress.key(slug, level))] = moves
        }
        updated[schemaKey] = CURRENT_SCHEMA
        return updated.toPreferences()
    }

    override suspend fun cleanUp() {}
}

interface ProgressRepository {
    val progress: Flow<Progress>
    suspend fun recordSolve(packSlug: String, level: Int, moves: Int): SolveResult
}

class DataStoreProgressRepository(private val context: Context) : ProgressRepository {

    override val progress: Flow<Progress> = context.progressStore.data
        // A corrupted store falls back to empty progress instead of crashing.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map(::toProgress)

    /**
     * Record a solve, keeping the lowest move count, and return the progress
     * either side of the write. Both snapshots come from inside the store
     * transaction, so unlock announcements derived from them are exact even
     * when solves land in quick succession.
     */
    override suspend fun recordSolve(packSlug: String, level: Int, moves: Int): SolveResult {
        var before = Progress(emptyMap())
        val after = context.progressStore.edit { prefs ->
            before = toProgress(prefs)
            val key: Preferences.Key<Int> = intPreferencesKey(Progress.key(packSlug, level))
            val existing = prefs[key]
            if (existing == null || moves < existing) prefs[key] = moves
        }
        return SolveResult(before, toProgress(after))
    }

    /** Only level entries count; ignores bookkeeping keys like the schema version. */
    private fun toProgress(prefs: Preferences): Progress = Progress(
        prefs.asMap()
            .mapNotNull { (key, value) ->
                if (!key.name.startsWith(Progress.KEY_PREFIX)) return@mapNotNull null
                (value as? Int)?.let { key.name to it }
            }
            .toMap(),
    )
}
