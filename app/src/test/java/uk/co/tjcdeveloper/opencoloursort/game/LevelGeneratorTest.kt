package uk.co.tjcdeveloper.opencoloursort.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    private val spec = LevelGenerator.Spec(colours = 4, emptyTubes = 2, minMovesRange = 7..10)

    @Test
    fun `generation is deterministic for a seed`() {
        val a = LevelGenerator.generate(spec, startSeed = 42)!!
        val b = LevelGenerator.generate(spec, startSeed = 42)!!
        assertEquals(a.encoded, b.encoded)
        assertEquals(a.seed, b.seed)
    }

    @Test
    fun `generated level is within the difficulty band`() {
        val generated = LevelGenerator.generate(spec, startSeed = 7)!!
        assertTrue(generated.minMoves in spec.minMovesRange)
        // And re-solving the emitted board agrees.
        val solved = Solver.solve(Board.parse(generated.encoded, spec.capacity))
        assertEquals(generated.minMoves, solved.minMoves)
    }

    @Test
    fun `generated level has correct structure`() {
        val generated = LevelGenerator.generate(spec, startSeed = 99)!!
        assertEquals(spec.colours + spec.emptyTubes, generated.encoded.size)
        assertEquals(spec.emptyTubes, generated.encoded.count { it.isEmpty() })
        val counts = generated.encoded.joinToString("").groupingBy { it }.eachCount()
        assertEquals(spec.colours, counts.size)
        assertTrue(counts.values.all { it == spec.capacity })
    }

    @Test
    fun `hard generation verifies solvability`() {
        val generated = LevelGenerator.generateHard(
            colours = 12, emptyTubes = 4, capacity = 12, startSeed = 1,
        )
        assertNotNull(generated)
        assertEquals(16, generated!!.encoded.size)
    }
}
