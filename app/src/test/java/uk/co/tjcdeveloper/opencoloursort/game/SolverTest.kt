package uk.co.tjcdeveloper.opencoloursort.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverTest {

    @Test
    fun `already solved board needs zero moves`() {
        val board = Board.parse(listOf("rrrr", "gggg", ""), capacity = 4)
        val result = Solver.solve(board)
        assertTrue(result.solvable)
        assertEquals(0, result.minMoves)
    }

    @Test
    fun `single pour to solve`() {
        val board = Board.parse(listOf("rrr", "r", ""), capacity = 4)
        val result = Solver.solve(board)
        assertTrue(result.solvable)
        assertEquals(1, result.minMoves)
    }

    @Test
    fun `two colour swap needs three moves`() {
        // rrgg / ggrr / empty: minimum is 3 pours.
        val board = Board.parse(listOf("rrgg", "ggrr", ""), capacity = 4)
        val result = Solver.solve(board)
        assertTrue(result.solvable)
        assertEquals(3, result.minMoves)
    }

    @Test
    fun `unsolvable when no empty space to work with`() {
        val board = Board.parse(listOf("rgrg", "grgr"), capacity = 4)
        val result = Solver.solve(board)
        assertFalse(result.solvable)
        assertFalse(result.truncated)
    }

    @Test
    fun `handoff level is solvable`() {
        val board = Board.parse(
            listOf("royy", "bpr", "ygbp", "org", "pbor", "gypb", "go", ""),
            capacity = 4,
        )
        val result = Solver.solve(board)
        assertTrue(result.solvable)
        assertNotNull(result.minMoves)
        assertTrue(result.minMoves!! > 0)
        assertTrue(Solver.isSolvable(board))
    }

    @Test
    fun `isSolvable agrees with solve on unsolvable board`() {
        val board = Board.parse(listOf("rgrg", "grgr"), capacity = 4)
        assertFalse(Solver.isSolvable(board))
    }
}
