package uk.co.tjcdeveloper.opencoloursort.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.progressStore by preferencesDataStore(name = "progress")

/**
 * Per-level progress: best (lowest) move count for each solved level.
 * A level is solved iff it has a best-move entry. Keys are "p<pack>_l<level>".
 */
data class Progress(val bestMoves: Map<String, Int>) {
    fun isSolved(pack: Int, level: Int): Boolean = key(pack, level) in bestMoves
    fun bestFor(pack: Int, level: Int): Int? = bestMoves[key(pack, level)]
    fun solvedInPack(pack: Int): Int = bestMoves.keys.count { it.startsWith("p${pack}_") }

    companion object {
        fun key(pack: Int, level: Int) = "p${pack}_l$level"
    }
}

class ProgressRepository(private val context: Context) {

    val progress: Flow<Progress> = context.progressStore.data.map { prefs ->
        Progress(
            prefs.asMap()
                .mapNotNull { (key, value) ->
                    (value as? Int)?.let { key.name to it }
                }
                .toMap(),
        )
    }

    /** Record a solve, keeping the lowest move count. */
    suspend fun recordSolve(pack: Int, level: Int, moves: Int) {
        val key: Preferences.Key<Int> = intPreferencesKey(Progress.key(pack, level))
        context.progressStore.edit { prefs ->
            val existing = prefs[key]
            if (existing == null || moves < existing) prefs[key] = moves
        }
    }
}
