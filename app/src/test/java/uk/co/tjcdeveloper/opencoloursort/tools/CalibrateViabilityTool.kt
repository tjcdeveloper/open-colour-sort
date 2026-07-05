package uk.co.tjcdeveloper.opencoloursort.tools

import uk.co.tjcdeveloper.opencoloursort.game.Board
import uk.co.tjcdeveloper.opencoloursort.game.LevelGenerator
import uk.co.tjcdeveloper.opencoloursort.game.Solver
import uk.co.tjcdeveloper.opencoloursort.game.Viability
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Calibration tool, not a test: samples random deals per candidate spec and
 * prints the distribution of optimal lengths AND dead-state share, so pack
 * bands for the full 11-pack progression are set from data. Run:
 *
 *   ./gradlew :app:testDebugUnitTest --tests "*CalibrateViability*" -PbakeLevels=true
 */
class CalibrateViabilityTool {

    @Test
    fun calibrate() {
        assumeTrue(System.getProperty("bakeLevels") == "true")
        val specs = listOf(
            6 to 2, 7 to 2, 8 to 2, 9 to 2, 10 to 2, 12 to 2,
            6 to 1, 7 to 1, 8 to 1, 10 to 1, 12 to 1,
        )
        for ((colours, empties) in specs) {
            sample(colours, empties)
        }
    }

    private fun sample(colours: Int, empties: Int, seeds: Int = 100) {
        val spec = LevelGenerator.Spec(colours, empties, minMovesRange = IntRange.EMPTY)
        val minMoves = ArrayList<Int>()
        val deadPercents = ArrayList<Double>()
        var unsolvable = 0
        var truncated = 0
        for (seed in 0 until seeds) {
            val board = Board.parse(LevelGenerator.deal(spec, 50_000L + seed), spec.capacity)
            if (board.isSolved || board.tubes.indices.any { board.isTubeSolved(it) }) continue
            val solved = Solver.solve(board, maxStates = 400_000)
            if (solved.minMoves == null) {
                if (solved.truncated) truncated++ else unsolvable++
                continue
            }
            minMoves.add(solved.minMoves!!)
            val stats = Viability.analyse(board, maxStates = 2_000_000)
            if (stats == null) truncated++ else deadPercents.add(stats.deadPercent)
        }
        minMoves.sort()
        deadPercents.sort()
        fun pMoves(q: Double) = if (minMoves.isEmpty()) "-" else "${minMoves[((minMoves.size - 1) * q).toInt()]}"
        fun pDead(q: Double) = if (deadPercents.isEmpty()) "-" else "%.1f".format(deadPercents[((deadPercents.size - 1) * q).toInt()])
        println(
            "${colours}c ${empties}e: solvable=${minMoves.size}/$seeds unsolvable=$unsolvable truncated=$truncated | " +
                "moves p25=${pMoves(0.25)} p50=${pMoves(0.5)} p75=${pMoves(0.75)} max=${pMoves(1.0)} | " +
                "dead% p10=${pDead(0.1)} p25=${pDead(0.25)} p50=${pDead(0.5)} p75=${pDead(0.75)} p90=${pDead(0.9)} max=${pDead(1.0)}",
        )
    }
}
