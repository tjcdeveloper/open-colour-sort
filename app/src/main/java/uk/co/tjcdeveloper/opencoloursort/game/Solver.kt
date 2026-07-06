package uk.co.tjcdeveloper.opencoloursort.game

import java.util.PriorityQueue

/**
 * Water-sort solver. Used to validate generated levels and measure
 * difficulty (minimum solution length); never on the UI thread.
 */
object Solver {

    data class Result(
        val solvable: Boolean,
        /** Minimum number of pours, when [solvable] and the search completed. */
        val minMoves: Int?,
        /** True if the search hit [maxStates] before finishing. */
        val truncated: Boolean,
    )

    /**
     * Admissible heuristic: every pour reduces the total number of
     * same-colour runs by at most one, and a solved board has exactly one
     * run per colour. So (runs - colours) is a lower bound on moves left.
     */
    private fun heuristic(tubes: List<List<GameColour>>): Int {
        var runs = 0
        val colours = HashSet<GameColour>()
        for (tube in tubes) {
            var prev: GameColour? = null
            for (c in tube) {
                colours.add(c)
                if (c != prev) {
                    runs++
                    prev = c
                }
            }
        }
        return runs - colours.size
    }

    private fun legalMoves(board: Board): List<Pair<Int, Int>> {
        val moves = ArrayList<Pair<Int, Int>>()
        var emptyTried = false
        for (to in board.tubes.indices) {
            // All empty tubes are interchangeable; only consider the first.
            val isEmpty = board.isTubeEmpty(to)
            if (isEmpty && emptyTried) continue
            if (isEmpty) emptyTried = true
            for (from in board.tubes.indices) {
                if (!board.canPour(from, to)) continue
                // Pointless: moving a complete run from one tube where it sits
                // alone into an empty tube.
                val run = board.topRun(from)!!
                if (isEmpty && run.count == board.tubes[from].size) continue
                moves.add(from to to)
            }
        }
        return moves
    }

    /** A* search for the minimum-move solution. */
    fun solve(board: Board, maxStates: Int = 200_000): Result {
        if (board.isSolved) return Result(solvable = true, minMoves = 0, truncated = false)

        data class Node(val board: Board, val g: Int, val f: Int)

        val open = PriorityQueue<Node>(compareBy { it.f })
        val bestG = HashMap<String, Int>()
        val startKey = board.canonicalKey()
        open.add(Node(board, 0, heuristic(board.tubes)))
        bestG[startKey] = 0
        var expanded = 0

        while (open.isNotEmpty()) {
            val node = open.poll()!!
            if (node.board.isSolved) {
                return Result(solvable = true, minMoves = node.g, truncated = false)
            }
            if (++expanded > maxStates) {
                return Result(solvable = false, minMoves = null, truncated = true)
            }
            val key = node.board.canonicalKey()
            if (node.g > (bestG[key] ?: Int.MAX_VALUE)) continue

            for ((from, to) in legalMoves(node.board)) {
                val next = node.board.pour(from, to)?.board ?: continue
                val nextKey = next.canonicalKey()
                val g = node.g + 1
                if (g < (bestG[nextKey] ?: Int.MAX_VALUE)) {
                    bestG[nextKey] = g
                    open.add(Node(next, g, g + heuristic(next.tubes)))
                }
            }
        }
        return Result(solvable = false, minMoves = null, truncated = false)
    }

    /** Fast solvability check: depth-first with visited-state pruning. */
    fun isSolvable(board: Board, maxStates: Int = 500_000): Boolean {
        val visited = HashSet<String>()
        val stack = ArrayDeque<Board>()
        stack.addLast(board)
        visited.add(board.canonicalKey())
        while (stack.isNotEmpty()) {
            if (visited.size > maxStates) return false
            val current = stack.removeLast()
            if (current.isSolved) return true
            for ((from, to) in legalMoves(current)) {
                val next = current.pour(from, to)?.board ?: continue
                if (visited.add(next.canonicalKey())) {
                    stack.addLast(next)
                }
            }
        }
        return false
    }
}
