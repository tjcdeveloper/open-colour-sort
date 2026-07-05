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

    /** Any legal pour available (including into empty tubes)? */
    fun hasAnyMove(): Boolean {
        for (from in board.tubes.indices) {
            for (to in board.tubes.indices) {
                if (board.canPour(from, to)) return true
            }
        }
        return false
    }

    /**
     * Stalemate: the level is unsolved and every remaining option is a dead
     * end — no legal pour can move a complete colour run (any pour would only
     * split one), and no extra tube remains to open the board up. Undo may
     * still be possible; callers decide whether to offer it.
     */
    val isStuck: Boolean
        get() = !isSolved && extraTubesRemaining <= 0 &&
            board.tubes.indices.none { from -> hasFullRunPour(from) }

    /** Some legal pour from [from] moves its entire top run. */
    private fun hasFullRunPour(from: Int): Boolean {
        val run = board.topRun(from) ?: return false
        return board.tubes.indices.any { to ->
            board.canPour(from, to) && board.freeSpace(to) >= run.count
        }
    }

    fun restart() {
        board = initial
        moveCount = 0
        undosUsed = 0
        extraTubesRemaining = extraTubesAllowed
        history.clear()
    }
}
