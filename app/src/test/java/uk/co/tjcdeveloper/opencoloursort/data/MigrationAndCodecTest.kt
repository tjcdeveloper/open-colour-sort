package uk.co.tjcdeveloper.opencoloursort.data

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import uk.co.tjcdeveloper.opencoloursort.game.Move
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationAndCodecTest {

    @Test
    fun `migration re-keys positional entries onto v1 slugs`() = runTest {
        val old = mutablePreferencesOf(
            intPreferencesKey("p0_l1") to 6,
            intPreferencesKey("p1_l40") to 20,
            intPreferencesKey("p10_l3") to 99,
        )
        val migrated = PositionalKeyMigration.migrate(old)
        assertEquals(6, migrated[intPreferencesKey("p_beginner_l1")])
        assertEquals(20, migrated[intPreferencesKey("p_easy-1_l40")])
        assertEquals(99, migrated[intPreferencesKey("p_final-challenge_l3")])
        assertNull(migrated[intPreferencesKey("p0_l1")])
        assertEquals(2, migrated[PositionalKeyMigration.schemaKey])
    }

    @Test
    fun `migration survives junk keys and runs once`() = runTest {
        val old = mutablePreferencesOf(
            intPreferencesKey("p99999999999999999999_l1") to 5, // overflow index
            intPreferencesKey("p42_l1") to 5, // no such v1 pack
            stringPreferencesKey("p0_l1") to "not-an-int",
        )
        assertTrue(PositionalKeyMigration.shouldMigrate(old))
        val migrated = PositionalKeyMigration.migrate(old)
        assertEquals(2, migrated[PositionalKeyMigration.schemaKey])
        // Stamped stores are never migrated again.
        assertFalse(PositionalKeyMigration.shouldMigrate(migrated))
    }

    @Test
    fun `history codec round-trips including the empty history`() {
        val history = listOf(Move(0, 6, 2), Move(12, 3, 1))
        assertEquals(history, SessionCodec.decodeHistory(SessionCodec.encodeHistory(history)))
        assertEquals(emptyList<Move>(), SessionCodec.decodeHistory(SessionCodec.encodeHistory(emptyList())))
        assertEquals("0>6:2,12>3:1", SessionCodec.encodeHistory(history))
    }

    @Test
    fun `history codec drops malformed entries instead of crashing`() {
        assertEquals(
            listOf(Move(1, 2, 3)),
            SessionCodec.decodeHistory("garbage,1>2:3,4>5,>>:,99999999999999999999>1:1"),
        )
    }

    @Test
    fun `tube encoding round-trips trailing empty tubes`() {
        // SavedSession stores tubes joined with '|'; trailing empties must survive.
        val tubes = listOf("rgby", "", "")
        assertEquals(tubes, tubes.joinToString("|").split('|'))
    }
}
