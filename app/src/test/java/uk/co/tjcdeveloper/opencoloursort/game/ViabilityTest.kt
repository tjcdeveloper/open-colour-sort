package uk.co.tjcdeveloper.opencoloursort.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViabilityTest {

    private fun analyse(vararg tubes: String): Viability.Stats =
        Viability.analyse(Board.parse(tubes.toList(), 4))!!

    @Test
    fun `solved board is one fully viable state`() {
        val stats = analyse("rrrr", "")
        assertEquals(1, stats.totalStates)
        assertEquals(0, stats.deadStates)
        assertEquals(0.0, stats.deadPercent, 0.0)
    }

    @Test
    fun `unsolvable board is fully dead`() {
        val stats = analyse("rgrg", "grgr")
        assertEquals(1, stats.totalStates)
        assertEquals(1, stats.deadStates)
        assertEquals(100.0, stats.deadPercent, 0.0)
    }

    @Test
    fun `solvable board keeps its start state viable`() {
        val stats = analyse("rrry", "yyyr", "")
        assertTrue(stats.totalStates > 1)
        assertTrue(stats.deadStates < stats.totalStates)
    }

    @Test
    fun `two-empty starter level has no dead states at all`() {
        // Pack 1 level 1's board (seed 1001): measured 78 states, none dead.
        val stats = analyse("yorr", "yryy", "rooo", "", "")
        assertEquals(78, stats.totalStates)
        assertEquals(0, stats.deadStates)
        assertTrue(stats.earlyDepths.all { it.dead == 0 })
    }

    @Test
    fun `early depth stats cover the first four moves`() {
        val stats = analyse("yorr", "yryy", "rooo", "", "")
        assertEquals(4, stats.earlyDepths.size)
        assertEquals(3, stats.earlyDepths[0].states) // three distinct first moves
    }
}
