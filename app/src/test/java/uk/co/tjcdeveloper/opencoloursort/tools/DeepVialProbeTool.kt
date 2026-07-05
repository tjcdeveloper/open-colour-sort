package uk.co.tjcdeveloper.opencoloursort.tools

import uk.co.tjcdeveloper.opencoloursort.game.LevelGenerator
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Probe tool, not a test: checks whether the "12 colours x 24-deep vials"
 * novelty boards can be generated and solvability-verified in sane time. Run:
 *
 *   ./gradlew :app:testDebugUnitTest --tests "*DeepVialProbe*" -PbakeLevels=true
 */
class DeepVialProbeTool {

    @Test
    fun probe() {
        assumeTrue(System.getProperty("bakeLevels") == "true")
        for (seed in 0L until 3L) {
            val startedAt = System.nanoTime()
            val generated = LevelGenerator.generateHard(
                colours = 12, emptyTubes = 4, capacity = 24,
                startSeed = 70_000L + seed, maxAttempts = 1,
            )
            val millis = (System.nanoTime() - startedAt) / 1_000_000
            println("capacity24 seed=${70_000L + seed}: ${if (generated != null) "SOLVABLE" else "not verified"} in ${millis}ms")
        }
    }
}
