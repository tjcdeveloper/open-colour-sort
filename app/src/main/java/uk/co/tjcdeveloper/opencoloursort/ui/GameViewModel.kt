package uk.co.tjcdeveloper.opencoloursort.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import uk.co.tjcdeveloper.opencoloursort.data.Progress
import uk.co.tjcdeveloper.opencoloursort.data.ProgressRepository
import uk.co.tjcdeveloper.opencoloursort.data.SavedSession
import uk.co.tjcdeveloper.opencoloursort.data.SessionRepository
import uk.co.tjcdeveloper.opencoloursort.game.Board
import uk.co.tjcdeveloper.opencoloursort.game.GameEngine
import uk.co.tjcdeveloper.opencoloursort.levels.Pack
import uk.co.tjcdeveloper.opencoloursort.levels.Packs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { GAME, LEVELS, SETTINGS }

data class GameUiState(
    val board: Board,
    val moveCount: Int,
    val selectedTube: Int?,
    val canUndo: Boolean,
    val undosUsed: Int,
    val undosRemaining: Int,
    val extraTubesRemaining: Int,
    val isSolved: Boolean,
    val isStuck: Boolean,
    val levelLabel: String,
    val packId: Int,
    val levelNumber: Int,
    val lastPour: PourEvent? = null,
    /** Pack names whose unlock threshold this solve crossed, for the win dialog. */
    val newlyUnlockedPacks: List<String> = emptyList(),
)

/** Emitted once per successful pour so the UI can animate/haptic it. */
data class PourEvent(val from: Int, val to: Int, val moved: Int, val id: Long)

class GameViewModel(
    private val progressRepository: ProgressRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val progress: StateFlow<Progress> = progressRepository.progress
        .stateIn(viewModelScope, SharingStarted.Eagerly, Progress(emptyMap()))

    private var pack: Pack = Packs.byId(0)
    private var levelNumber: Int = 1
    private var engine = newEngine()
    private var pourCounter = 0L
    private var solveRecorded = false

    var screen by mutableStateOf(Screen.GAME)
    var uiState by mutableStateOf(snapshot())
        private set

    init {
        // Resume the persisted in-progress game if there is one; otherwise
        // the first unsolved level of the first incomplete pack. One-shot:
        // later progress changes must not yank the player around.
        viewModelScope.launch {
            if (uiState.moveCount != 0) return@launch
            val saved = sessionRepository.session.first()
            if (saved != null && restoreSession(saved)) return@launch
            val stored = progressRepository.progress.first()
            val (targetPack, targetLevel) = firstUnsolved(stored)
            if (targetPack != pack.id || targetLevel != levelNumber) {
                loadLevel(targetPack, targetLevel)
            }
        }
    }

    /** Rebuild the engine from a saved mid-level session. False if stale. */
    private fun restoreSession(saved: SavedSession): Boolean {
        val savedPack = Packs.all.firstOrNull { it.slug == saved.packSlug } ?: return false
        if (saved.level !in 1..savedPack.levels.size) return false
        val current = runCatching { Board.parse(saved.tubes, savedPack.capacity) }
            .getOrNull() ?: return false
        if (current.isSolved) return false
        pack = savedPack
        levelNumber = saved.level
        engine = GameEngine.restore(
            initialBoard = Board.parse(savedPack.levels[saved.level - 1], savedPack.capacity),
            currentBoard = current,
            moveCount = saved.moveCount,
            undosUsed = saved.undosUsed,
            extraTubesRemaining = saved.extraTubesRemaining,
            history = saved.history,
        )
        solveRecorded = false
        uiState = snapshot()
        return true
    }

    private fun firstUnsolved(progress: Progress): Pair<Int, Int> {
        for (candidate in Packs.all.filter { !it.isHard }) {
            for (level in 1..candidate.levels.size) {
                if (!progress.isSolved(candidate.slug, level)) return candidate.id to level
            }
        }
        return 0 to 1
    }

    private fun newEngine(): GameEngine =
        GameEngine(Board.parse(pack.levels[levelNumber - 1], pack.capacity))

    fun loadLevel(packId: Int, level: Int) {
        pack = Packs.byId(packId)
        levelNumber = level.coerceIn(1, pack.levels.size)
        engine = newEngine()
        solveRecorded = false
        uiState = snapshot()
        screen = Screen.GAME
        persistSession()
    }

    fun nextLevel() {
        if (levelNumber < pack.levels.size) {
            loadLevel(pack.id, levelNumber + 1)
        } else {
            // Pack finished: move to level 1 of the next pack in play order
            // (classic packs flow into the hard series), or stay for replay.
            val next = Packs.nextPack(pack.id)
            if (next != null) loadLevel(next.id, 1) else loadLevel(pack.id, levelNumber)
        }
    }

    /** First tap selects, second tap pours (or reselects). */
    fun onTubeTapped(index: Int) {
        if (engine.isSolved || engine.isStuck) return
        val selected = uiState.selectedTube
        when {
            selected == null -> {
                if (engine.board.tubes[index].isNotEmpty() && !engine.board.isTubeSolved(index)) {
                    uiState = snapshot().copy(selectedTube = index)
                }
            }
            selected == index -> uiState = snapshot().copy(selectedTube = null)
            engine.canPour(selected, index) -> {
                val moved = engine.pour(selected, index)
                uiState = snapshot().copy(
                    lastPour = PourEvent(selected, index, moved, ++pourCounter),
                )
                if (engine.isSolved) {
                    onSolved()
                } else {
                    persistSession()
                }
            }
            else -> {
                val newSelection = if (engine.board.tubes[index].isNotEmpty()) index else null
                uiState = snapshot().copy(selectedTube = newSelection)
            }
        }
    }

    private fun onSolved() {
        if (solveRecorded) return
        solveRecorded = true
        val solvedPack = pack
        val level = levelNumber
        val moves = engine.moveCount
        viewModelScope.launch {
            sessionRepository.clear()
            // Announcements derive from the write's own before/after
            // snapshots, so a threshold fires exactly once even across
            // rapid solves.
            val result = progressRepository.recordSolve(solvedPack.slug, level, moves)
            val unlocked = Packs.newlyUnlocked(
                before = { id -> result.before.solvedInPack(Packs.byId(id).slug) },
                after = { id -> result.after.solvedInPack(Packs.byId(id).slug) },
            ).map { it.name }
            if (unlocked.isNotEmpty() && engine.isSolved) {
                uiState = uiState.copy(newlyUnlockedPacks = unlocked)
            }
        }
    }

    fun onUndo() {
        if (engine.isSolved) return
        if (engine.undo()) {
            uiState = snapshot()
            persistSession()
        }
    }

    fun onRestart() {
        engine.restart()
        solveRecorded = false
        uiState = snapshot()
        persistSession()
    }

    fun onExtraTube() {
        if (engine.isSolved) return
        if (engine.useExtraTube()) {
            uiState = snapshot()
            persistSession()
        }
    }

    /** Write the live session so process death resumes mid-level. */
    private fun persistSession() {
        val session = SavedSession(
            packSlug = pack.slug,
            level = levelNumber,
            tubes = engine.board.encode(),
            moveCount = engine.moveCount,
            undosUsed = engine.undosUsed,
            extraTubesRemaining = engine.extraTubesRemaining,
            history = engine.historySnapshot(),
        )
        viewModelScope.launch { sessionRepository.save(session) }
    }

    private fun snapshot() = GameUiState(
        board = engine.board,
        moveCount = engine.moveCount,
        selectedTube = null,
        canUndo = engine.canUndo,
        undosUsed = engine.undosUsed,
        undosRemaining = engine.undosRemaining,
        extraTubesRemaining = engine.extraTubesRemaining,
        isSolved = engine.isSolved,
        isStuck = engine.isStuck,
        levelLabel = Packs.levelLabel(pack, levelNumber),
        packId = pack.id,
        levelNumber = levelNumber,
    )
}
