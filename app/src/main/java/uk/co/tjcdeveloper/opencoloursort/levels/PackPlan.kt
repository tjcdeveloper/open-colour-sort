package uk.co.tjcdeveloper.opencoloursort.levels

import uk.co.tjcdeveloper.opencoloursort.game.LevelGenerator

/**
 * The difficulty curve for the shipped packs. Each classic pack is a list of
 * segments; the bake tool generates and verifies a board for every level
 * from these plans, and the results are frozen into [GeneratedLevels].
 * Regenerate with the BakeLevels tool after changing.
 *
 * Difficulty runs on two solver-verified ramps: number of colours, and the
 * dead-state share ([uk.co.tjcdeveloper.opencoloursort.game.Viability]) -
 * the fraction of reachable positions from which a win is no longer
 * possible, which tracks how trap-laden a level feels. The min-moves band
 * keeps levels honest within each tier. All bands sit inside the sampled
 * p10-p90 ranges from CalibrateViabilityTool (100 deals per spec):
 *
 *   dead% medians, 2-empty: 6c 2.5 / 7c 6.2 / 8c 10.2 / 9c 15.6 / 10c 21.1 / 12c 30.2
 *   1-empty boards are corridors: few are solvable at all (6c 19%, 7c 8%)
 *   and their dead share is huge (medians 38-46%) - used sparingly, late,
 *   and never beyond 7 colours (solvable deals all but vanish by 8c).
 */
object PackPlan {

    data class Segment(val count: Int, val spec: LevelGenerator.Spec)

    data class ClassicPack(val name: String, val segments: List<Segment>) {
        val levelCount: Int get() = segments.sumOf { it.count }
    }

    private fun segment(
        count: Int,
        colours: Int,
        empties: Int,
        minMoves: IntRange,
        dead: ClosedFloatingPointRange<Double>,
    ) = Segment(
        count,
        LevelGenerator.Spec(colours, empties, minMovesRange = minMoves, deadPercentRange = dead),
    )

    /** Packs 1-10, in play order. Each is 40 levels. */
    val classicPacks: List<ClassicPack> = listOf(
        ClassicPack(
            "Beginner",
            listOf(
                segment(3, 3, 2, 4..7, 0.0..1.0),
                segment(5, 4, 2, 7..10, 0.0..2.0),
                segment(3, 5, 2, 11..14, 0.0..2.0),
                segment(3, 5, 2, 11..14, 2.0..4.0),
                segment(8, 6, 2, 14..18, 0.5..3.0),
                segment(8, 6, 2, 15..19, 3.0..5.9),
                segment(6, 6, 2, 16..20, 5.0..7.0),
                segment(4, 6, 2, 17..21, 6.5..8.0),
            ),
        ),
        ClassicPack(
            "Easy 1",
            listOf(
                segment(8, 6, 2, 15..19, 3.0..6.0),
                segment(8, 6, 2, 16..20, 5.0..8.0),
                segment(12, 7, 2, 19..23, 4.0..8.0),
                segment(12, 7, 2, 19..24, 7.0..10.0),
            ),
        ),
        ClassicPack(
            "Easy 2",
            listOf(
                segment(10, 7, 2, 19..23, 6.0..9.0),
                segment(10, 7, 2, 19..24, 8.0..11.0),
                segment(10, 7, 2, 20..24, 10.0..12.5),
                segment(10, 7, 2, 20..24, 11.0..14.0),
            ),
        ),
        ClassicPack(
            "Intermediate 1",
            listOf(
                segment(10, 8, 2, 23..27, 8.0..11.0),
                segment(10, 8, 2, 23..28, 10.0..13.0),
                segment(10, 8, 2, 24..28, 12.0..14.5),
                segment(10, 8, 2, 24..28, 13.0..16.0),
            ),
        ),
        ClassicPack(
            "Intermediate 2",
            listOf(
                segment(10, 8, 2, 24..28, 14.0..17.0),
                segment(10, 9, 2, 26..31, 12.0..16.0),
                segment(10, 9, 2, 26..32, 15.0..18.0),
                segment(10, 9, 2, 27..32, 17.0..20.0),
            ),
        ),
        ClassicPack(
            "Hard 1",
            listOf(
                segment(10, 9, 2, 26..31, 17.0..20.0),
                segment(10, 9, 2, 26..32, 19.0..22.0),
                segment(10, 9, 2, 27..32, 21.0..24.0),
                segment(10, 9, 2, 27..32, 22.0..26.0),
            ),
        ),
        ClassicPack(
            "Hard 2",
            listOf(
                segment(10, 10, 2, 29..34, 19.0..23.0),
                segment(10, 10, 2, 29..35, 22.0..26.0),
                segment(10, 10, 2, 30..35, 24.0..28.0),
                segment(10, 10, 2, 30..35, 26.0..30.0),
            ),
        ),
        ClassicPack(
            "Hard 3",
            listOf(
                segment(8, 10, 2, 30..35, 24.0..28.0),
                segment(4, 6, 1, 13..18, 10.0..25.0),
                segment(10, 10, 2, 30..35, 27.0..32.0),
                segment(4, 6, 1, 14..19, 20.0..30.0),
                segment(10, 10, 2, 30..35, 30.0..35.0),
                segment(4, 7, 1, 18..24, 25.0..30.0),
            ),
        ),
        ClassicPack(
            "Extreme 1",
            listOf(
                segment(10, 12, 2, 34..42, 24.0..30.0),
                segment(4, 7, 1, 18..24, 30.0..40.0),
                segment(10, 12, 2, 35..43, 28.0..34.0),
                segment(4, 6, 1, 14..19, 35.0..45.0),
                segment(12, 12, 2, 35..44, 32.0..40.0),
            ),
        ),
        ClassicPack(
            "Extreme 2",
            listOf(
                segment(10, 12, 2, 35..44, 34.0..40.0),
                segment(5, 6, 1, 14..20, 40.0..55.0),
                segment(10, 12, 2, 35..44, 38.0..45.0),
                segment(5, 7, 1, 18..24, 45.0..60.0),
                segment(10, 12, 2, 36..45, 42.0..50.0),
            ),
        ),
    )

    /** Final Challenge: the handoff's 12 colours x 12 layers, 16 tubes. */
    const val HARD_LEVELS = 100
    const val HARD_COLOURS = 12
    const val HARD_EMPTY = 4
    const val HARD_CAPACITY = 12
}
