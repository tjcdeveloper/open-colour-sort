package uk.co.tjcdeveloper.opencoloursort.tools

import uk.co.tjcdeveloper.opencoloursort.game.Board
import uk.co.tjcdeveloper.opencoloursort.game.GameColour
import uk.co.tjcdeveloper.opencoloursort.game.Solver
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Calibration tool, not a test: samples random boards per spec and prints
 * the distribution of optimal solution lengths, so PackPlan bands are set
 * from data. Run: ./gradlew :app:testDebugUnitTest --tests "*Calibrate*" -PbakeLevels=true
 */
class CalibrateDifficultyTool {

    @Test
    fun calibrate() {
        assumeTrue(System.getProperty("bakeLevels") == "true")
        for ((colours, empties) in listOf(3 to 2, 4 to 2, 5 to 2, 6 to 2, 6 to 1)) {
            val lengths = ArrayList<Int>()
            var unsolvable = 0
            for (seed in 0 until 200) {
                val board = randomBoard(colours, empties, seed.toLong())
                val result = Solver.solve(board)
                if (result.minMoves != null) lengths.add(result.minMoves!!) else unsolvable++
            }
            lengths.sort()
            val p = { q: Double -> lengths[((lengths.size - 1) * q).toInt()] }
            println(
                "${colours}c ${empties}e: n=${lengths.size} unsolvable=$unsolvable " +
                    "min=${lengths.first()} p25=${p(0.25)} p50=${p(0.5)} p75=${p(0.75)} " +
                    "p90=${p(0.9)} max=${lengths.last()}",
            )
        }
    }

    private fun randomBoard(colours: Int, empties: Int, seed: Long): Board {
        val random = Random(seed * 31 + colours * 7 + empties)
        val pool = ArrayList<Char>()
        GameColour.entries.take(colours).forEach { c -> repeat(4) { pool.add(c.key) } }
        pool.shuffle(random)
        val tubes = (0 until colours).map {
            String(pool.subList(it * 4, it * 4 + 4).toCharArray())
        } + List(empties) { "" }
        return Board.parse(tubes, 4)
    }
}
