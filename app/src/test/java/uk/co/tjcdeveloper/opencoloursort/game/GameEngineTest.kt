package uk.co.tjcdeveloper.opencoloursort.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun engine(vararg tubes: String, capacity: Int = 4, extraTubes: Int = 1) =
        GameEngine(Board.parse(tubes.toList(), capacity), extraTubes)

    @Test
    fun `pour increments move count`() {
        val e = engine("ry", "y", "")
        assertEquals(1, e.pour(0, 1))
        assertEquals(1, e.moveCount)
    }

    @Test
    fun `illegal pour moves nothing and does not count`() {
        val e = engine("rb", "y")
        assertEquals(0, e.pour(0, 1))
        assertEquals(0, e.moveCount)
    }

    @Test
    fun `undo restores exact previous state including split runs`() {
        val e = engine("ryy", "by", "")
        val before = e.board
        e.pour(0, 1) // moves only 1 of the 2 yellows? capacity 4: tube1 has 2, free 2, run 2 -> moves 2
        assertTrue(e.undo())
        assertEquals(before, e.board)
        assertEquals(0, e.moveCount)
        assertEquals(1, e.undosUsed)
    }

    @Test
    fun `undo restores partial pour correctly`() {
        // Tube 1 has only 1 free slot; pouring 2 yellows moves just 1.
        val e = engine("ryy", "byy", "")
        e.pour(0, 1)
        assertEquals(listOf(GameColour.RED, GameColour.YELLOW), e.board.tubes[0])
        assertTrue(e.undo())
        assertEquals(
            listOf(GameColour.RED, GameColour.YELLOW, GameColour.YELLOW),
            e.board.tubes[0],
        )
        assertEquals(3, e.board.tubes[1].size)
    }

    @Test
    fun `undo with no history returns false`() {
        val e = engine("ry", "")
        assertFalse(e.undo())
    }

    @Test
    fun `undo across multiple moves rewinds in order`() {
        val e = engine("rg", "g", "", "")
        val initial = e.board
        e.pour(0, 1) // g onto g
        e.pour(0, 2) // r into empty
        assertTrue(e.undo())
        assertTrue(e.undo())
        assertEquals(initial, e.board)
    }

    @Test
    fun `extra tube adds one empty tube and is limited`() {
        val e = engine("ry", extraTubes = 1)
        assertEquals(1, e.extraTubesRemaining)
        assertTrue(e.useExtraTube())
        assertEquals(2, e.board.tubes.size)
        assertFalse(e.useExtraTube())
        assertEquals(2, e.board.tubes.size)
    }

    @Test
    fun `restart restores initial board counts and extra tubes`() {
        val e = engine("rg", "g", "", extraTubes = 1)
        e.pour(0, 1)
        e.useExtraTube()
        e.restart()
        assertEquals(3, e.board.tubes.size)
        assertEquals(0, e.moveCount)
        assertEquals(1, e.extraTubesRemaining)
        assertFalse(e.canUndo)
    }

    @Test
    fun `solving a board is detected`() {
        val e = engine("rrrg", "g", "")
        e.pour(0, 1) // g -> g
        assertFalse(e.isSolved)
        // rrr + g g? tube0 = rrr, tube1 = gg, tube2 empty -- not solved (not full)
        // finish: not solvable to full here; use a proper solvable case instead
        val e2 = engine("rrr", "r", "")
        e2.pour(1, 0)
        assertTrue(e2.isSolved)
    }

    @Test
    fun `hasAnyMove false when stuck`() {
        // Two full tubes of alternating colours, no empties: no legal pour.
        val e = engine("rgrg", "grgr")
        assertFalse(e.hasAnyMove())
    }

    @Test
    fun `hasAnyMove true when an empty tube exists`() {
        val e = engine("rgrg", "grgr", "")
        assertTrue(e.hasAnyMove())
    }

    @Test
    fun `stuck when no legal pour exists`() {
        val e = engine("rgrg", "grgr", extraTubes = 0)
        assertTrue(e.isStuck)
    }

    @Test
    fun `stuck when every legal pour would split a run`() {
        // Only move: 3-red run from tube 0 into tube 1's single free slot.
        val e = engine("grrr", "ggr", extraTubes = 0)
        assertTrue(e.isStuck)
    }

    @Test
    fun `not stuck while a full run still fits somewhere`() {
        val e = engine("grr", "gr", extraTubes = 0)
        assertFalse(e.isStuck)
    }

    @Test
    fun `not stuck while the extra tube is unused`() {
        val e = engine("rgrg", "grgr", extraTubes = 1)
        assertFalse(e.isStuck)
        assertTrue(e.useExtraTube())
        assertFalse(e.isStuck) // the new empty tube takes any full run
    }

    @Test
    fun `solved board is not stuck`() {
        val e = engine("rrrr", "", extraTubes = 0)
        assertFalse(e.isStuck)
    }

    @Test
    fun `undo is limited to five per level`() {
        val e = engine("rg", "g", "")
        repeat(5) {
            assertEquals(1, e.pour(0, 1))
            assertTrue(e.undo())
        }
        e.pour(0, 1)
        assertFalse(e.canUndo)
        assertFalse(e.undo())
        assertEquals(5, e.undosUsed)
    }

    @Test
    fun `restart resets undo allowance`() {
        val e = engine("rg", "g", "")
        e.pour(0, 1)
        e.undo()
        e.restart()
        assertEquals(0, e.undosUsed)
        assertEquals(5, e.undosRemaining)
    }
}
