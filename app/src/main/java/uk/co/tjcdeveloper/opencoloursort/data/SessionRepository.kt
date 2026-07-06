package uk.co.tjcdeveloper.opencoloursort.data

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import uk.co.tjcdeveloper.opencoloursort.game.Move
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.sessionStore by preferencesDataStore(name = "session")

/**
 * A snapshot of the level being played, so a game in progress survives
 * process death. The board is the handoff string encoding; history is the
 * undo stack, newest last.
 */
data class SavedSession(
    val packSlug: String,
    val level: Int,
    val tubes: List<String>,
    val moveCount: Int,
    val undosUsed: Int,
    val extraTubesRemaining: Int,
    val history: List<Move>,
)

interface SessionRepository {
    val session: Flow<SavedSession?>
    suspend fun save(session: SavedSession)
    suspend fun clear()
}

class DataStoreSessionRepository(private val context: Context) : SessionRepository {

    private object Keys {
        val packSlug = stringPreferencesKey("pack_slug")
        val level = intPreferencesKey("level")
        val tubes = stringPreferencesKey("tubes")
        val moveCount = intPreferencesKey("move_count")
        val undosUsed = intPreferencesKey("undos_used")
        val extraTubesRemaining = intPreferencesKey("extra_tubes_remaining")
        val history = stringPreferencesKey("history")
    }

    override val session: Flow<SavedSession?> = context.sessionStore.data
        // A corrupted store means no resumable session, never a crash.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val packSlug = prefs[Keys.packSlug] ?: return@map null
            val level = prefs[Keys.level] ?: return@map null
            val tubes = prefs[Keys.tubes]?.split('|') ?: return@map null
            SavedSession(
                packSlug = packSlug,
                level = level,
                tubes = tubes,
                moveCount = prefs[Keys.moveCount] ?: 0,
                undosUsed = prefs[Keys.undosUsed] ?: 0,
                extraTubesRemaining = prefs[Keys.extraTubesRemaining] ?: 0,
                history = decodeHistory(prefs[Keys.history].orEmpty()),
            )
        }

    override suspend fun save(session: SavedSession) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.packSlug] = session.packSlug
            prefs[Keys.level] = session.level
            prefs[Keys.tubes] = session.tubes.joinToString("|")
            prefs[Keys.moveCount] = session.moveCount
            prefs[Keys.undosUsed] = session.undosUsed
            prefs[Keys.extraTubesRemaining] = session.extraTubesRemaining
            prefs[Keys.history] = session.history
                .joinToString(",") { "${it.from}>${it.to}:${it.moved}" }
        }
    }

    override suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }

    private fun decodeHistory(encoded: String): List<Move> =
        encoded.split(',').filter { it.isNotEmpty() }.mapNotNull { entry ->
            val match = Regex("""(\d+)>(\d+):(\d+)""").matchEntire(entry) ?: return@mapNotNull null
            val (from, to, moved) = match.destructured
            Move(from.toInt(), to.toInt(), moved.toInt())
        }
}
