package uk.co.tjcdeveloper.opencoloursort.tools

import uk.co.tjcdeveloper.opencoloursort.game.Board
import uk.co.tjcdeveloper.opencoloursort.game.GameColour
import uk.co.tjcdeveloper.opencoloursort.game.LevelGenerator
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Analysis tool, not a test: measures how many generator seeds deal an
 * UNSOLVABLE board per spec, distinguishing proven-unsolvable (reachable
 * state space exhausted without a solution) from search truncation. Run:
 *
 *   ./gradlew :app:testDebugUnitTest --tests "*SeedSolvabilityTool*" -PbakeLevels=true
 */
class SeedSolvabilityTool {

    private enum class Outcome { SOLVABLE, UNSOLVABLE, TRUNCATED, DEGENERATE }

    @Test
    fun scan() {
        assumeTrue(System.getProperty("bakeLevels") == "true")
        val seeds = 1_000L until 6_000L
        for (colours in 3..6) {
            for (empties in listOf(2, 1)) {
                scanSpec(colours, empties, seeds)
            }
        }
    }

    private fun scanSpec(colours: Int, empties: Int, seeds: LongRange) {
        val spec = LevelGenerator.Spec(colours, empties, minMovesRange = IntRange.EMPTY)
        val counts = HashMap<Outcome, Int>()
        val unsolvableSeeds = ArrayList<Long>()
        for (seed in seeds) {
            val board = Board.parse(LevelGenerator.deal(spec, seed), spec.capacity)
            val outcome = when {
                board.isSolved || board.tubes.indices.any { board.isTubeSolved(it) } ->
                    Outcome.DEGENERATE
                else -> prove(board)
            }
            counts.merge(outcome, 1, Int::plus)
            if (outcome == Outcome.UNSOLVABLE && unsolvableSeeds.size < 10) {
                unsolvableSeeds.add(seed)
            }
        }
        val total = seeds.count()
        println(
            "${colours}c ${empties}e: solvable=${counts[Outcome.SOLVABLE] ?: 0}/$total " +
                "unsolvable=${counts[Outcome.UNSOLVABLE] ?: 0} " +
                "truncated=${counts[Outcome.TRUNCATED] ?: 0} " +
                "degenerate=${counts[Outcome.DEGENERATE] ?: 0} " +
                "unsolvable-examples=$unsolvableSeeds",
        )
    }

    /** DFS to exhaustion: SOLVABLE, proven UNSOLVABLE, or TRUNCATED at cap. */
    private fun prove(board: Board, maxStates: Int = 2_000_000): Outcome {
        val visited = HashSet<String>()
        val stack = ArrayDeque<Board>()
        stack.addLast(board)
        visited.add(key(board))
        while (stack.isNotEmpty()) {
            if (visited.size > maxStates) return Outcome.TRUNCATED
            val current = stack.removeLast()
            if (current.isSolved) return Outcome.SOLVABLE
            for (from in current.tubes.indices) {
                for (to in current.tubes.indices) {
                    val next = current.pour(from, to)?.board ?: continue
                    if (visited.add(key(next))) stack.addLast(next)
                }
            }
        }
        return Outcome.UNSOLVABLE
    }

    private fun key(board: Board): String =
        board.tubes.map { tube -> String(tube.map(GameColour::key).toCharArray()) }
            .sorted()
            .joinToString("|")
}
