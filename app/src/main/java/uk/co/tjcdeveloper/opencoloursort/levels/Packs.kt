package uk.co.tjcdeveloper.opencoloursort.levels

/**
 * A shipped level pack. Classic packs use capacity-4 tubes; the Final
 * Challenge uses the handoff's 12-colour x 12-layer boards.
 */
data class Pack(
    val id: Int,
    /** Stable identifier used in progress keys - never rename or reuse. */
    val slug: String,
    val name: String,
    val levels: List<List<String>>,
    val capacity: Int,
    val isHard: Boolean,
)

object Packs {

    private const val FINAL_CHALLENGE_ID = 10

    /** All shipped packs, in play order: finishing one flows into the next. */
    val all: List<Pack> =
        PackPlan.classicPacks.mapIndexed { index, plan ->
            Pack(index, plan.slug, plan.name, GeneratedLevels.classic[index], capacity = 4, isHard = false)
        } + Pack(
            FINAL_CHALLENGE_ID, "final-challenge", "Final Challenge", GeneratedLevels.hard,
            capacity = PackPlan.HARD_CAPACITY, isHard = true,
        )

    /** Progress needed in an earlier pack before a pack unlocks. */
    private data class UnlockRule(val inPackId: Int, val solves: Int)

    /**
     * Beginner and the Easy tiers unlock the next pack at 25% (10 solves);
     * Intermediate 2 through Hard 3 need 50% (20) of the previous pack;
     * Extreme 1 needs 75% (30) of Hard 3; Extreme 2 needs all of Extreme 1.
     * The Final Challenge opens alongside Intermediate 1, at 25% of Easy 2.
     */
    private val unlockRules: Map<Int, UnlockRule> = mapOf(
        1 to UnlockRule(inPackId = 0, solves = 10),
        2 to UnlockRule(inPackId = 1, solves = 10),
        3 to UnlockRule(inPackId = 2, solves = 10),
        4 to UnlockRule(inPackId = 3, solves = 20),
        5 to UnlockRule(inPackId = 4, solves = 20),
        6 to UnlockRule(inPackId = 5, solves = 20),
        7 to UnlockRule(inPackId = 6, solves = 20),
        8 to UnlockRule(inPackId = 7, solves = 30),
        9 to UnlockRule(inPackId = 8, solves = 40),
        FINAL_CHALLENGE_ID to UnlockRule(inPackId = 2, solves = 10),
    )

    fun byId(id: Int): Pack = all.first { it.id == id }

    /** The pack that follows [packId] in play order, or null after the last. */
    fun nextPack(packId: Int): Pack? =
        all.getOrNull(all.indexOfFirst { it.id == packId } + 1)

    fun isUnlocked(pack: Pack, solvedInPack: (Int) -> Int): Boolean {
        val rule = unlockRules[pack.id] ?: return true
        return solvedInPack(rule.inPackId) >= rule.solves
    }

    /**
     * Packs that unlock when solve counts move from [before] to [after], in
     * play order. Solve counts only grow, so a pack appears here exactly
     * once - on the solve that crosses its threshold - which makes this the
     * "announce a new pack" signal with no stored flag needed.
     */
    fun newlyUnlocked(before: (Int) -> Int, after: (Int) -> Int): List<Pack> =
        all.filter { !isUnlocked(it, before) && isUnlocked(it, after) }

    /** Level label per the handoff: "27" classic, "3 · HARD" hard. */
    fun levelLabel(pack: Pack, levelNumber: Int): String =
        if (pack.isHard) "$levelNumber · HARD" else levelNumber.toString()
}
