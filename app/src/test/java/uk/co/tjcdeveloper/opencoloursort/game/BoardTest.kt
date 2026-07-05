package uk.co.tjcdeveloper.opencoloursort.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    // The handoff's mid-game classic level, capacity 4.
    private val handoffLevel = listOf("royy", "bpr", "ygbp", "org", "pbor", "gypb", "go", "")

    @Test
    fun `parse maps keys bottom to top`() {
        val board = Board.parse(handoffLevel, capacity = 4)
        assertEquals(
            listOf(GameColour.RED, GameColour.ORANGE, GameColour.YELLOW, GameColour.YELLOW),
            board.tubes[0],
        )
        assertTrue(board.tubes[7].isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse rejects unknown colour key`() {
        Board.parse(listOf("rz"), capacity = 4)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `overfull tube rejected`() {
        Board.parse(listOf("rrrrr"), capacity = 4)
    }

    @Test
    fun `topRun counts identical top segments`() {
        val board = Board.parse(listOf("royy"), capacity = 4)
        assertEquals(TopRun(GameColour.YELLOW, 2), board.topRun(0))
    }

    @Test
    fun `topRun of empty tube is null`() {
        val board = Board.parse(listOf(""), capacity = 4)
        assertNull(board.topRun(0))
    }

    @Test
    fun `pour into empty tube moves whole top run`() {
        val board = Board.parse(listOf("royy", "y", ""), capacity = 4)
        val result = board.pour(0, 2)!!
        assertEquals(2, result.moved)
        assertEquals(listOf(GameColour.YELLOW, GameColour.YELLOW), result.board.tubes[2])
    }

    @Test
    fun `pour moves entire top run onto matching colour`() {
        val board = Board.parse(listOf("royy", "y", ""), capacity = 4)
        val result = board.pour(0, 1)!!
        assertEquals(2, result.moved)
        assertEquals(listOf(GameColour.RED, GameColour.ORANGE), result.board.tubes[0])
        assertEquals(3, result.board.tubes[1].size)
    }

    @Test
    fun `pour is truncated by free space`() {
        // Source has a run of three yellows but the target only has one free slot.
        val board = Board.parse(listOf("ryyy", "byy"), capacity = 4)
        val result = board.pour(0, 1)!!
        assertEquals(1, result.moved)
        assertEquals(3, result.board.tubes[0].size)
        assertEquals(4, result.board.tubes[1].size)
    }

    @Test
    fun `cannot pour onto non-matching colour`() {
        val board = Board.parse(listOf("ry", "rb"), capacity = 4)
        assertFalse(board.canPour(0, 1))
    }

    @Test
    fun `can pour any colour into empty tube`() {
        val board = Board.parse(listOf("ry", ""), capacity = 4)
        assertTrue(board.canPour(0, 1))
    }

    @Test
    fun `cannot pour into full tube`() {
        val board = Board.parse(listOf("yy", "yyyy"), capacity = 4)
        assertFalse(board.canPour(0, 1))
    }

    @Test
    fun `cannot pour from empty tube or onto itself`() {
        val board = Board.parse(listOf("", "r"), capacity = 4)
        assertFalse(board.canPour(0, 1))
        assertFalse(board.canPour(1, 1))
    }

    @Test
    fun `cannot pour out of a solved tube`() {
        val board = Board.parse(listOf("yyyy", ""), capacity = 4)
        assertFalse(board.canPour(0, 1))
    }

    @Test
    fun `solved when all tubes uniform-full or empty`() {
        assertTrue(Board.parse(listOf("rrrr", "yyyy", "", ""), capacity = 4).isSolved)
        assertFalse(Board.parse(listOf("rrry", "yyyr", "", ""), capacity = 4).isSolved)
        assertFalse(Board.parse(listOf("rrr", "yyyy", "r", ""), capacity = 4).isSolved)
    }

    @Test
    fun `partial single-colour tube is not solved`() {
        // Uniform but not full to capacity does not count as solved.
        assertFalse(Board.parse(listOf("rrrr", "yyy", "y"), capacity = 4).isSolved)
    }

    @Test
    fun `withExtraTube appends an empty tube`() {
        val board = Board.parse(listOf("ry"), capacity = 4).withExtraTube()
        assertEquals(2, board.tubes.size)
        assertTrue(board.tubes[1].isEmpty())
    }

    @Test
    fun `hard mode capacity twelve`() {
        val tube = "rrrrrrrrrrrr" // 12 segments
        val board = Board.parse(listOf(tube, ""), capacity = 12)
        assertTrue(board.isTubeSolved(0))
    }
}
