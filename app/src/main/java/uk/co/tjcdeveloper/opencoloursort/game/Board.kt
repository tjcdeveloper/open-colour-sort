package uk.co.tjcdeveloper.opencoloursort.game

/**
 * Immutable board state. Each tube is a list of colours, bottom -> top,
 * matching the handoff's level encoding order.
 */
data class Board(
    val tubes: List<List<GameColour>>,
    val capacity: Int,
) {
    init {
        require(capacity > 0) { "Capacity must be positive" }
        require(tubes.all { it.size <= capacity }) { "Tube exceeds capacity" }
    }

    /** The colour and length of the run of identical colours at the top of a tube. */
    fun topRun(index: Int): TopRun? {
        val tube = tubes[index]
        val top = tube.lastOrNull() ?: return null
        var count = 0
        for (i in tube.indices.reversed()) {
            if (tube[i] != top) break
            count++
        }
        return TopRun(top, count)
    }

    fun isTubeSolved(index: Int): Boolean {
        val tube = tubes[index]
        return tube.size == capacity && tube.all { it == tube.first() }
    }

    fun isTubeEmpty(index: Int): Boolean = tubes[index].isEmpty()

    /** Solved when every tube is either empty or full of a single colour. */
    val isSolved: Boolean
        get() = tubes.indices.all { isTubeEmpty(it) || isTubeSolved(it) }

    fun freeSpace(index: Int): Int = capacity - tubes[index].size

    /**
     * A pour is legal when the source has liquid, the target has space, the
     * tubes differ, and the target is empty or its top colour matches the
     * source's. Pouring out of an already-solved tube is pointless and
     * blocked, as is a pour that only shifts a full run into a different
     * empty tube... except that moving a run to an empty tube is a real
     * strategy, so only the solved-tube case is blocked.
     */
    fun canPour(from: Int, to: Int): Boolean {
        if (from == to) return false
        val run = topRun(from) ?: return false
        if (isTubeSolved(from)) return false
        if (freeSpace(to) == 0) return false
        val targetTop = tubes[to].lastOrNull()
        return targetTop == null || targetTop == run.colour
    }

    /**
     * Pour as much of the top run as fits (standard water-sort behaviour).
     * Returns the new board and the number of segments moved, or null if
     * the pour is illegal.
     */
    fun pour(from: Int, to: Int): PourResult? {
        if (!canPour(from, to)) return null
        val run = topRun(from) ?: return null
        val moved = minOf(run.count, freeSpace(to))
        val newTubes = tubes.mapIndexed { i, tube ->
            when (i) {
                from -> tube.subList(0, tube.size - moved).toList()
                to -> tube + List(moved) { run.colour }
                else -> tube
            }
        }
        return PourResult(copy(tubes = newTubes), moved)
    }

    /** Board with one extra empty tube appended (the extra-tube power-up). */
    fun withExtraTube(): Board = copy(tubes = tubes + listOf(emptyList()))

    companion object {
        /**
         * Parse the handoff's encoding: one string per tube, each character a
         * colour key, bottom -> top. Empty string = empty tube.
         */
        fun parse(encoded: List<String>, capacity: Int): Board =
            Board(encoded.map { tube -> tube.map(GameColour::fromKey) }, capacity)
    }
}

data class TopRun(val colour: GameColour, val count: Int)

data class PourResult(val board: Board, val moved: Int)
