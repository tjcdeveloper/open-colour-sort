package uk.co.tjcdeveloper.opencoloursort.tools

import uk.co.tjcdeveloper.opencoloursort.game.Board
import uk.co.tjcdeveloper.opencoloursort.game.GameColour
import uk.co.tjcdeveloper.opencoloursort.levels.GeneratedLevels
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Analysis tool, not a test: writes level-viability.csv (repo root) with one
 * row per classic level, measuring "trap density" - of all reachable states,
 * how many are DEAD (a win is no longer reachable from them), and how deadly
 * the first four moves are. Run explicitly:
 *
 *   ./gradlew :app:testDebugUnitTest --tests "*LevelStatsTool*" -PbakeLevels=true
 *
 * States differing only by tube order count as one state; the extra-tube
 * power-up is NOT applied (base board only). Hard levels' state spaces are
 * too large to exhaust and are skipped.
 */
class LevelStatsTool {

    @Test
    fun writeViabilityStats() {
        assumeTrue(System.getProperty("bakeLevels") == "true")

        val seeds = Regex("""\(seed (\d+)\)""")
            .findAll(File("src/main/java/uk/co/tjcdeveloper/opencoloursort/levels/GeneratedLevels.kt").readText())
            .map { it.groupValues[1] }
            .toList()

        val out = StringBuilder()
        out.appendLine(
            "Seed,Pack,Level,Num Colours,Num Empty Vials,Total States,Dead States,Dead %," +
                "D1 States,D1 Dead %,D2 States,D2 Dead %,D3 States,D3 Dead %,D4 States,D4 Dead %",
        )

        var seedIndex = 0
        for ((packIndex, levels) in GeneratedLevels.classic.withIndex()) {
            val packNumber = packIndex + 1
            for ((index, encoded) in levels.withIndex()) {
                val seed = seeds.getOrNull(seedIndex++) ?: ""
                val board = Board.parse(encoded, 4)
                val colours = board.tubes.flatten().distinct().size
                val empties = board.tubes.count { it.isEmpty() }
                val row = analyse(board)
                out.appendLine("$seed,$packNumber,${index + 1},$colours,$empties,$row")
                println("pack$packNumber L${index + 1}: $row")
            }
        }

        val target = File(System.getProperty("levelStats.out") ?: "../level-viability.csv")
        target.writeText(out.toString())
        println("Wrote ${target.absolutePath}")
    }

    private fun analyse(start: Board): String {
        // Forward exhaustion of the reachable graph, remembering each state's
        // successors and its first-reach depth.
        val successors = HashMap<String, List<String>>()
        val firstDepth = HashMap<String, Int>()
        val solvedKeys = ArrayList<String>()
        var frontier = listOf(start)
        firstDepth[key(start)] = 0
        var depth = 0
        while (frontier.isNotEmpty()) {
            depth++
            val next = ArrayList<Board>()
            for (board in frontier) {
                val boardKey = key(board)
                if (board.isSolved) {
                    solvedKeys.add(boardKey)
                    successors[boardKey] = emptyList()
                    continue
                }
                val distinct = HashMap<String, Board>()
                for (from in board.tubes.indices) {
                    for (to in board.tubes.indices) {
                        val poured = board.pour(from, to)?.board ?: continue
                        distinct[key(poured)] = poured
                    }
                }
                successors[boardKey] = distinct.keys.toList()
                for ((succKey, succBoard) in distinct) {
                    if (succKey !in firstDepth) {
                        firstDepth[succKey] = depth
                        next.add(succBoard)
                    }
                }
            }
            frontier = next
        }

        // Backward reachability from the solved states = the viable set.
        val predecessors = HashMap<String, MutableList<String>>()
        for ((from, tos) in successors) {
            for (to in tos) predecessors.getOrPut(to) { ArrayList() }.add(from)
        }
        val viable = HashSet<String>(solvedKeys)
        val queue = ArrayDeque(solvedKeys)
        while (queue.isNotEmpty()) {
            for (pred in predecessors[queue.removeFirst()] ?: emptyList()) {
                if (viable.add(pred)) queue.add(pred)
            }
        }

        val total = firstDepth.size
        val dead = total - viable.size
        val perDepth = (1..4).joinToString(",") { d ->
            val atDepth = firstDepth.filterValues { it == d }.keys
            val deadAtDepth = atDepth.count { it !in viable }
            "${atDepth.size},${percent(deadAtDepth, atDepth.size)}"
        }
        return "$total,$dead,${percent(dead, total)},$perDepth"
    }

    private fun percent(part: Int, whole: Int): String =
        if (whole == 0) "0.0" else "%.1f".format(part * 100.0 / whole)

    private fun key(board: Board): String =
        board.tubes.map { tube -> String(tube.map(GameColour::key).toCharArray()) }
            .sorted()
            .joinToString("|")
}
