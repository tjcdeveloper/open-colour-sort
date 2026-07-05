package uk.co.tjcdeveloper.opencoloursort.game

/**
 * Dead-state analysis: exhausts a board's reachable state graph and counts
 * the states from which a win is no longer reachable. The dead-state share
 * is a difficulty signal - it measures how trap-laden a level is, which
 * tracks perceived difficulty better than optimal solution length. Offline
 * use only (level generation and analysis tools), never on the UI thread.
 */
object Viability {

    data class Stats(
        val totalStates: Int,
        val deadStates: Int,
        /** Per move depth 1..4: states first reached at that depth, and how many are dead. */
        val earlyDepths: List<DepthStats>,
    ) {
        val deadPercent: Double
            get() = if (totalStates == 0) 0.0 else deadStates * 100.0 / totalStates
    }

    data class DepthStats(val states: Int, val dead: Int)

    private const val EARLY_DEPTHS = 4

    /**
     * Exhaust the reachable graph (tube order ignored) and mark viability by
     * backward reachability from the solved states. Returns null if the graph
     * exceeds [maxStates] - too large to analyse exactly.
     */
    fun analyse(board: Board, maxStates: Int = 500_000): Stats? {
        val successors = HashMap<String, List<String>>()
        val firstDepth = HashMap<String, Int>()
        val solvedKeys = ArrayList<String>()
        var frontier = listOf(board)
        firstDepth[canonical(board)] = 0
        var depth = 0
        while (frontier.isNotEmpty()) {
            if (firstDepth.size > maxStates) return null
            depth++
            val next = ArrayList<Board>()
            for (current in frontier) {
                val key = canonical(current)
                if (current.isSolved) {
                    solvedKeys.add(key)
                    successors[key] = emptyList()
                    continue
                }
                val distinct = HashMap<String, Board>()
                for (from in current.tubes.indices) {
                    for (to in current.tubes.indices) {
                        val poured = current.pour(from, to)?.board ?: continue
                        distinct[canonical(poured)] = poured
                    }
                }
                successors[key] = distinct.keys.toList()
                for ((successorKey, successor) in distinct) {
                    if (successorKey !in firstDepth) {
                        firstDepth[successorKey] = depth
                        next.add(successor)
                    }
                }
            }
            frontier = next
        }

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

        val earlyDepths = (1..EARLY_DEPTHS).map { d ->
            val atDepth = firstDepth.filterValues { it == d }.keys
            DepthStats(atDepth.size, atDepth.count { it !in viable })
        }
        return Stats(firstDepth.size, firstDepth.size - viable.size, earlyDepths)
    }

    private fun canonical(board: Board): String =
        board.tubes.map { tube -> String(tube.map { it.key }.toCharArray()) }
            .sorted()
            .joinToString("|")
}
