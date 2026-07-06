package uk.co.tjcdeveloper.opencoloursort.game

/**
 * One entry in the undo history: the pour that was made and how many
 * segments it moved, so it can be reversed exactly.
 */
data class Move(val from: Int, val to: Int, val moved: Int)

/**
 * Mutable game session over immutable [Board] states: applies pours, tracks
 * move count and undo history, and the limited extra-tube power-up.
 */
class GameEngine(
    initialBoard: Board,
    private val extraTubesAllowed: Int = 1,
    private val undosAllowed: Int = 5,
) {
    var board: Board = initialBoard
        private set
    var moveCount: Int = 0
        private set
    var undosUsed: Int = 0
        private set
    var extraTubesRemaining: Int = extraTubesAllowed
        private set

    private val initial = initialBoard
    private val history = ArrayDeque<Move>()

    val isSolved: Boolean get() = board.isSolved
    val undosRemaining: Int get() = undosAllowed - undosUsed
    val canUndo: Boolean get() = history.isNotEmpty() && undosRemaining > 0

    fun canPour(from: Int, to: Int): Boolean = board.canPour(from, to)

    /** Apply a pour. Returns the number of segments moved, or 0 if illegal. */
    fun pour(from: Int, to: Int): Int {
        val result = board.pour(from, to) ?: return 0
        board = result.board
        moveCount++
        history.addLast(Move(from, to, result.moved))
        return result.moved
    }

    /** Reverse the last pour. Returns true if a move was undone. */
    fun undo(): Boolean {
        if (undosRemaining <= 0) return false
        val last = history.removeLastOrNull() ?: return false
        val tubes = board.tubes.toMutableList()
        val target = tubes[last.to]
        val colour = target.last()
        tubes[last.to] = target.subList(0, target.size - last.moved).toList()
        tubes[last.from] = tubes[last.from] + List(last.moved) { colour }
        board = board.copy(tubes = tubes)
        moveCount--
        undosUsed++
        return true
    }

    /** Add one empty tube if any remain. Returns true on success. */
    fun useExtraTube(): Boolean {
        if (extraTubesRemaining <= 0) return false
        extraTubesRemaining--
        board = board.withExtraTube()
        return true
    }

    /**
     * Stalemate: the level is unsolved and no legal pour can move a complete
     * colour run (any pour would only split one). The extra tube or an undo
     * may still rescue the game; callers offer whichever escapes remain.
     *
     * Invariant (why this never fires on a winnable board): a partial pour
     * needs a target holding capacity-f segments of a colour and a source
     * run r > f, totalling more than [Board.capacity] of that colour - but a
     * well-formed board has exactly capacity of each, and empty tubes always
     * fit a whole run. So on shipped levels every legal pour moves its full
     * run, and isStuck is true exactly when no legal pour exists at all.
     */
    val isStuck: Boolean
        get() = !isSolved && board.tubes.indices.none { from -> hasFullRunPour(from) }

    /** Some legal pour from [from] moves its entire top run. */
    private fun hasFullRunPour(from: Int): Boolean {
        val run = board.topRun(from) ?: return false
        return board.tubes.indices.any { to ->
            board.canPour(from, to) && board.freeSpace(to) >= run.count
        }
    }

    /** The undo history, oldest first, for session persistence. */
    fun historySnapshot(): List<Move> = history.toList()

    fun restart() {
        board = initial
        moveCount = 0
        undosUsed = 0
        extraTubesRemaining = extraTubesAllowed
        history.clear()
    }

    companion object {
        /**
         * Rebuild a mid-level session from persisted state. [initialBoard]
         * stays the level's pristine board so restart still works.
         */
        fun restore(
            initialBoard: Board,
            currentBoard: Board,
            moveCount: Int,
            undosUsed: Int,
            extraTubesRemaining: Int,
            history: List<Move>,
        ): GameEngine {
            val engine = GameEngine(initialBoard)
            engine.board = currentBoard
            engine.moveCount = moveCount
            engine.undosUsed = undosUsed
            engine.extraTubesRemaining = extraTubesRemaining
            engine.history.addAll(history)
            return engine
        }
    }
}
