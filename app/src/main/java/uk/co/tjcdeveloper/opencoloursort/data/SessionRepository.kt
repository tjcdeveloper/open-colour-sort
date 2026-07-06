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
    /** The level's pristine encoding; a rebaked level invalidates the session. */
    val initialTubes: List<String>,
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
        val initialTubes = stringPreferencesKey("initial_tubes")
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
            val initialTubes = prefs[Keys.initialTubes]?.split('|') ?: return@map null
            SavedSession(
                packSlug = packSlug,
                level = level,
                initialTubes = initialTubes,
                tubes = tubes,
                moveCount = prefs[Keys.moveCount] ?: 0,
                undosUsed = prefs[Keys.undosUsed] ?: 0,
                extraTubesRemaining = prefs[Keys.extraTubesRemaining] ?: 0,
                history = SessionCodec.decodeHistory(prefs[Keys.history].orEmpty()),
            )
        }

    override suspend fun save(session: SavedSession) {
        context.sessionStore.edit { prefs ->
            prefs[Keys.packSlug] = session.packSlug
            prefs[Keys.level] = session.level
            prefs[Keys.initialTubes] = session.initialTubes.joinToString("|")
            prefs[Keys.tubes] = session.tubes.joinToString("|")
            prefs[Keys.moveCount] = session.moveCount
            prefs[Keys.undosUsed] = session.undosUsed
            prefs[Keys.extraTubesRemaining] = session.extraTubesRemaining
            prefs[Keys.history] = SessionCodec.encodeHistory(session.history)
        }
    }

    override suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }
}

/** Undo-history wire format: "from>to:moved" entries, comma separated. */
internal object SessionCodec {
    private val entry = Regex("""(\d+)>(\d+):(\d+)""")

    fun encodeHistory(history: List<Move>): String =
        history.joinToString(",") { "${it.from}>${it.to}:${it.moved}" }

    fun decodeHistory(encoded: String): List<Move> =
        encoded.split(',').filter { it.isNotEmpty() }.mapNotNull { text ->
            val match = entry.matchEntire(text) ?: return@mapNotNull null
            val (from, to, moved) = match.destructured
            Move(from.toIntOrNull() ?: return@mapNotNull null,
                to.toIntOrNull() ?: return@mapNotNull null,
                moved.toIntOrNull() ?: return@mapNotNull null)
        }
}
