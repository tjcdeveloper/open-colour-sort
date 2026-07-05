package uk.co.tjcdeveloper.opencoloursort.game

import kotlin.random.Random

/**
 * Seeded level generator: shuffle-then-verify. Deals colour segments
 * randomly into the filled tubes, rejects boards that are unsolvable,
 * trivially arranged, or outside the requested difficulty band, and
 * retries with the next seed. Used offline to bake the shipped level set;
 * deterministic for a given seed.
 */
object LevelGenerator {

    data class Spec(
        val colours: Int,
        val emptyTubes: Int,
        val capacity: Int = 4,
        /** Accepted band for the solver's minimum solution length. */
        val minMovesRange: IntRange,
        /** Accepted band for the share of dead states ([Viability]), if set. */
        val deadPercentRange: ClosedFloatingPointRange<Double>? = null,
    )

    data class Generated(
        val board: Board,
        /** Tube encoding, bottom -> top, matching the handoff format. */
        val encoded: List<String>,
        val seed: Long,
        val minMoves: Int,
        /** Dead-state share, computed only when the spec sets a band. */
        val deadPercent: Double? = null,
    )

    /**
     * Generate a level for [spec], trying successive seeds from [startSeed]
     * until one verifies. Returns null if [maxAttempts] seeds all fail
     * (loosen the band or add attempts).
     */
    fun generate(spec: Spec, startSeed: Long, maxAttempts: Int = 400): Generated? {
        require(spec.colours <= GameColour.entries.size)
        for (attempt in 0 until maxAttempts) {
            val seed = startSeed + attempt
            val encoded = deal(spec, seed)
            val board = Board.parse(encoded, spec.capacity)
            if (board.isSolved) continue
            // No tube may start already complete - feels broken to players.
            if (board.tubes.indices.any { board.isTubeSolved(it) }) continue
            // Dead-band first: it is the tighter filter, and an unsolvable
            // deal shows up as ~100% dead so it never reaches the solver.
            var deadPercent: Double? = null
            val deadRange = spec.deadPercentRange
            if (deadRange != null) {
                deadPercent = Viability.analyse(board)?.deadPercent ?: continue
                if (deadPercent !in deadRange) continue
            }
            val result = Solver.solve(board, maxStates = 400_000)
            val minMoves = result.minMoves ?: continue
            if (minMoves in spec.minMovesRange) {
                return Generated(board, encoded, seed, minMoves, deadPercent)
            }
        }
        return null
    }

    /** Deal colours*capacity segments into `colours` full tubes, plus empties. */
    internal fun deal(spec: Spec, seed: Long): List<String> {
        val random = Random(seed)
        val pool = ArrayList<Char>(spec.colours * spec.capacity)
        GameColour.entries.take(spec.colours).forEach { colour ->
            repeat(spec.capacity) { pool.add(colour.key) }
        }
        pool.shuffle(random)
        val tubes = ArrayList<String>()
        for (i in 0 until spec.colours) {
            val from = i * spec.capacity
            tubes.add(String(pool.subList(from, from + spec.capacity).toCharArray()))
        }
        repeat(spec.emptyTubes) { tubes.add("") }
        return tubes
    }

    /**
     * Verify a hard-mode board (capacity 12) is solvable. Optimal search is
     * infeasible at this size; DFS solvability is the acceptance bar.
     */
    fun generateHard(
        colours: Int,
        emptyTubes: Int,
        capacity: Int,
        startSeed: Long,
        maxAttempts: Int = 50,
    ): Generated? {
        val spec = Spec(colours, emptyTubes, capacity, IntRange.EMPTY)
        for (attempt in 0 until maxAttempts) {
            val seed = startSeed + attempt
            val encoded = deal(spec, seed)
            val board = Board.parse(encoded, capacity)
            if (board.isSolved) continue
            if (board.tubes.indices.any { board.isTubeSolved(it) }) continue
            if (Solver.isSolvable(board, maxStates = 2_000_000)) {
                return Generated(board, encoded, seed, minMoves = -1)
            }
        }
        return null
    }
}
