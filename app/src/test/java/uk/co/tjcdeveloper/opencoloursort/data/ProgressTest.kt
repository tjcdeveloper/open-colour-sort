package uk.co.tjcdeveloper.opencoloursort.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    @Test
    fun `keys are slug scoped`() {
        assertEquals("p_easy-1_l12", Progress.key("easy-1", 12))
    }

    @Test
    fun `solved and best lookups match keys`() {
        val progress = Progress(mapOf(Progress.key("beginner", 3) to 8))
        assertTrue(progress.isSolved("beginner", 3))
        assertFalse(progress.isSolved("beginner", 4))
        assertFalse(progress.isSolved("easy-1", 3))
        assertEquals(8, progress.bestFor("beginner", 3))
        assertNull(progress.bestFor("beginner", 4))
    }

    @Test
    fun `pack counts do not bleed across similar slugs`() {
        val progress = Progress(
            mapOf(
                Progress.key("hard-1", 1) to 10,
                Progress.key("hard-1", 2) to 10,
                Progress.key("hard-2", 1) to 10,
            ),
        )
        assertEquals(2, progress.solvedInPack("hard-1"))
        assertEquals(1, progress.solvedInPack("hard-2"))
        assertEquals(0, progress.solvedInPack("hard"))
    }
}
