package uk.co.tjcdeveloper.opencoloursort.levels

import uk.co.tjcdeveloper.opencoloursort.game.Board
import uk.co.tjcdeveloper.opencoloursort.game.GameColour
import uk.co.tjcdeveloper.opencoloursort.game.Solver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedLevelsTest {

    /** Progress stub: solve counts per pack id, zero elsewhere. */
    private fun solved(vararg counts: Pair<Int, Int>): (Int) -> Int {
        val byPack = counts.toMap()
        return { byPack[it] ?: 0 }
    }

    @Test
    fun `packs have the planned sizes`() {
        assertEquals(10, GeneratedLevels.classic.size)
        for ((index, plan) in PackPlan.classicPacks.withIndex()) {
            assertEquals(plan.name, 40, plan.levelCount)
            assertEquals(plan.name, plan.levelCount, GeneratedLevels.classic[index].size)
        }
        assertEquals(PackPlan.HARD_LEVELS, GeneratedLevels.hard.size)
    }

    @Test
    fun `every classic level parses is unsolved and is solvable`() {
        for ((packIndex, pack) in GeneratedLevels.classic.withIndex()) {
            for ((i, encoded) in pack.withIndex()) {
                val board = Board.parse(encoded, capacity = 4)
                assertFalse("P${packIndex + 1} L${i + 1} starts solved", board.isSolved)
                assertFalse(
                    "P${packIndex + 1} L${i + 1} has a pre-solved tube",
                    board.tubes.indices.any { board.isTubeSolved(it) },
                )
                assertTrue(
                    "P${packIndex + 1} L${i + 1} is not solvable",
                    Solver.isSolvable(board),
                )
            }
        }
    }

    @Test
    fun `classic levels have exactly four segments per colour`() {
        for (pack in GeneratedLevels.classic) {
            for (encoded in pack) {
                val counts = encoded.joinToString("").groupingBy { it }.eachCount()
                assertTrue(counts.values.all { it == 4 })
            }
        }
    }

    @Test
    fun `hard levels have the handoff structure`() {
        // 16 tubes: 12 full at capacity 12, 4 empty; 12 segments per colour.
        for ((i, encoded) in GeneratedLevels.hard.withIndex()) {
            assertEquals("H${i + 1} tube count", 16, encoded.size)
            assertEquals("H${i + 1} full tubes", 12, encoded.count { it.length == 12 })
            assertEquals("H${i + 1} empty tubes", 4, encoded.count { it.isEmpty() })
            val counts = encoded.joinToString("").groupingBy { it }.eachCount()
            assertEquals("H${i + 1} colour count", 12, counts.size)
            assertTrue("H${i + 1} segments per colour", counts.values.all { it == 12 })
            // Parses with every key valid.
            Board.parse(encoded, capacity = 12)
        }
    }

    @Test
    fun `packs unlock per the progression rules`() {
        assertTrue(Packs.isUnlocked(Packs.byId(0), solved()))
        // Beginner and Easy tiers: 25% (10 solves) of the previous pack.
        assertFalse(Packs.isUnlocked(Packs.byId(1), solved(0 to 9)))
        assertTrue(Packs.isUnlocked(Packs.byId(1), solved(0 to 10)))
        assertTrue(Packs.isUnlocked(Packs.byId(3), solved(2 to 10)))
        // Intermediate 2 through Hard 3: 50% (20 solves) of the previous pack.
        assertFalse(Packs.isUnlocked(Packs.byId(4), solved(3 to 19)))
        assertTrue(Packs.isUnlocked(Packs.byId(4), solved(3 to 20)))
        assertTrue(Packs.isUnlocked(Packs.byId(7), solved(6 to 20)))
        // Extreme 1: 75% (30) of Hard 3; Extreme 2: all of Extreme 1.
        assertFalse(Packs.isUnlocked(Packs.byId(8), solved(7 to 29)))
        assertTrue(Packs.isUnlocked(Packs.byId(8), solved(7 to 30)))
        assertFalse(Packs.isUnlocked(Packs.byId(9), solved(8 to 39)))
        assertTrue(Packs.isUnlocked(Packs.byId(9), solved(8 to 40)))
        // Final Challenge opens alongside Intermediate 1, at 25% of Easy 2.
        assertFalse(Packs.isUnlocked(Packs.byId(10), solved(2 to 9)))
        assertTrue(Packs.isUnlocked(Packs.byId(10), solved(2 to 10)))
    }

    @Test
    fun `packs follow each other in play order`() {
        assertEquals(1, Packs.nextPack(0)?.id)
        assertEquals(9, Packs.nextPack(8)?.id)
        assertEquals(10, Packs.nextPack(9)?.id)
        assertEquals(null, Packs.nextPack(10))
    }

    @Test
    fun `level labels match the handoff`() {
        assertEquals("27", Packs.levelLabel(Packs.byId(0), 27))
        assertEquals("3 · HARD", Packs.levelLabel(Packs.byId(10), 3))
    }

    @Test
    fun `all level keys are valid colours`() {
        val valid = GameColour.entries.map { it.key }.toSet()
        for (pack in Packs.all) {
            for (encoded in pack.levels) {
                assertTrue(encoded.joinToString("").all { it in valid })
            }
        }
    }
}
