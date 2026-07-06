package uk.co.tjcdeveloper.opencoloursort.ui

import uk.co.tjcdeveloper.opencoloursort.data.Progress
import uk.co.tjcdeveloper.opencoloursort.data.ProgressRepository
import uk.co.tjcdeveloper.opencoloursort.data.SavedSession
import uk.co.tjcdeveloper.opencoloursort.data.SessionRepository
import uk.co.tjcdeveloper.opencoloursort.data.SolveResult
import uk.co.tjcdeveloper.opencoloursort.game.Board
import uk.co.tjcdeveloper.opencoloursort.game.Move
import uk.co.tjcdeveloper.opencoloursort.levels.GeneratedLevels
import uk.co.tjcdeveloper.opencoloursort.levels.Packs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeProgressRepository(initial: Map<String, Int> = emptyMap()) : ProgressRepository {
        val state = MutableStateFlow(Progress(initial))
        override val progress: Flow<Progress> = state
        override suspend fun recordSolve(packSlug: String, level: Int, moves: Int): SolveResult {
            val before = state.value
            val key = Progress.key(packSlug, level)
            val existing = before.bestMoves[key]
            val after = if (existing == null || moves < existing) {
                Progress(before.bestMoves + (key to moves))
            } else {
                before
            }
            state.value = after
            return SolveResult(before, after)
        }
    }

    private class FakeSessionRepository(initial: SavedSession? = null) : SessionRepository {
        val state = MutableStateFlow(initial)
        override val session: Flow<SavedSession?> = state
        override suspend fun save(session: SavedSession) { state.value = session }
        override suspend fun clear() { state.value = null }
    }

    private fun solvedLevels(packSlug: String, count: Int): Map<String, Int> =
        (1..count).associate { Progress.key(packSlug, it) to 10 }

    /** DFS pour sequence that solves [board]; the levels are all solvable. */
    private fun solvePath(board: Board, seen: HashSet<String> = HashSet()): List<Pair<Int, Int>>? {
        if (board.isSolved) return emptyList()
        if (!seen.add(board.canonicalKey())) return null
        for (from in board.tubes.indices) {
            for (to in board.tubes.indices) {
                val next = board.pour(from, to)?.board ?: continue
                solvePath(next, seen)?.let { return listOf(from to to) + it }
            }
        }
        return null
    }

    private fun solveCurrentLevel(viewModel: GameViewModel) {
        val path = solvePath(viewModel.uiState.board)!!
        for ((from, to) in path) {
            viewModel.onTubeTapped(from)
            viewModel.onTubeTapped(to)
        }
    }

    @Test
    fun `resumes at the first unsolved level`() = runTest(dispatcher.scheduler) {
        val viewModel = GameViewModel(
            FakeProgressRepository(solvedLevels("beginner", 3)),
            FakeSessionRepository(),
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.uiState.packId)
        assertEquals(4, viewModel.uiState.levelNumber)
    }

    @Test
    fun `resumes into the next pack once a pack is complete`() = runTest(dispatcher.scheduler) {
        val viewModel = GameViewModel(
            FakeProgressRepository(solvedLevels("beginner", 40)),
            FakeSessionRepository(),
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.packId)
        assertEquals(1, viewModel.uiState.levelNumber)
    }

    @Test
    fun `restores a saved mid-level session`() = runTest(dispatcher.scheduler) {
        // Build a genuine one-pour-in position for Beginner level 5.
        val start = Board.parse(GeneratedLevels.classic[0][4], 4)
        var pour: Triple<Int, Int, Board>? = null
        outer@ for (from in start.tubes.indices) {
            for (to in start.tubes.indices) {
                val result = start.pour(from, to) ?: continue
                pour = Triple(from, to, result.board)
                break@outer
            }
        }
        val (from, to, poured) = pour!!
        val saved = SavedSession(
            packSlug = "beginner", level = 5,
            initialTubes = GeneratedLevels.classic[0][4], tubes = poured.encode(),
            moveCount = 1, undosUsed = 0, extraTubesRemaining = 1,
            history = listOf(Move(from, to, poured.tubes[to].size - start.tubes[to].size)),
        )
        val viewModel = GameViewModel(FakeProgressRepository(), FakeSessionRepository(saved))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, viewModel.uiState.levelNumber)
        assertEquals(1, viewModel.uiState.moveCount)
        assertTrue(viewModel.uiState.canUndo)
        assertEquals(poured, viewModel.uiState.board)
    }

    @Test
    fun `a session from a rebaked level set is discarded`() = runTest(dispatcher.scheduler) {
        val saved = SavedSession(
            packSlug = "beginner", level = 5,
            initialTubes = listOf("rrrr", "yyyy", ""), // no longer matches the shipped level
            tubes = listOf("rrr", "yyyy", "r"),
            moveCount = 1, undosUsed = 0, extraTubesRemaining = 1, history = emptyList(),
        )
        val viewModel = GameViewModel(FakeProgressRepository(), FakeSessionRepository(saved))
        dispatcher.scheduler.advanceUntilIdle()
        // Falls back to first-unsolved resume instead of the stale board.
        assertEquals(1, viewModel.uiState.levelNumber)
        assertEquals(0, viewModel.uiState.moveCount)
    }

    @Test
    fun `a pour persists the in-progress session`() = runTest(dispatcher.scheduler) {
        val session = FakeSessionRepository()
        val viewModel = GameViewModel(FakeProgressRepository(), session)
        dispatcher.scheduler.advanceUntilIdle()

        val (from, to) = solvePath(viewModel.uiState.board)!!.first()
        viewModel.onTubeTapped(from)
        viewModel.onTubeTapped(to)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, session.state.value?.moveCount)
        assertEquals("beginner", session.state.value?.packSlug)
        assertEquals(1, session.state.value?.level)
    }

    @Test
    fun `solving a level records progress and clears the session`() = runTest(dispatcher.scheduler) {
        val progress = FakeProgressRepository()
        val session = FakeSessionRepository()
        val viewModel = GameViewModel(progress, session)
        dispatcher.scheduler.advanceUntilIdle()

        solveCurrentLevel(viewModel)
        assertTrue(viewModel.uiState.isSolved)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(progress.state.value.isSolved("beginner", 1))
        assertNull(session.state.value)
    }

    @Test
    fun `crossing an unlock threshold announces the pack once`() = runTest(dispatcher.scheduler) {
        val progress = FakeProgressRepository(solvedLevels("beginner", 9))
        val viewModel = GameViewModel(progress, FakeSessionRepository())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(10, viewModel.uiState.levelNumber)

        solveCurrentLevel(viewModel)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("Easy 1"), viewModel.uiState.newlyUnlockedPacks)

        // Replaying the same level announces nothing new.
        viewModel.loadLevel(0, 10)
        solveCurrentLevel(viewModel)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.newlyUnlockedPacks.isEmpty())
    }

    @Test
    fun `undo and extra vial are inert on a solved board`() = runTest(dispatcher.scheduler) {
        val viewModel = GameViewModel(FakeProgressRepository(), FakeSessionRepository())
        dispatcher.scheduler.advanceUntilIdle()
        solveCurrentLevel(viewModel)
        val solvedMoves = viewModel.uiState.moveCount

        viewModel.onUndo()
        viewModel.onExtraTube()
        assertTrue(viewModel.uiState.isSolved)
        assertEquals(solvedMoves, viewModel.uiState.moveCount)
        assertEquals(1, viewModel.uiState.extraTubesRemaining)
    }

    @Test
    fun `next level after the last flows into the following pack`() = runTest(dispatcher.scheduler) {
        val viewModel = GameViewModel(FakeProgressRepository(), FakeSessionRepository())
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.loadLevel(0, Packs.byId(0).levels.size)
        viewModel.nextLevel()
        assertEquals(1, viewModel.uiState.packId)
        assertEquals(1, viewModel.uiState.levelNumber)
    }
}
