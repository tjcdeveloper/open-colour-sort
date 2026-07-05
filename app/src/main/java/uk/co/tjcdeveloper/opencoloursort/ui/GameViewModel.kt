package uk.co.tjcdeveloper.opencoloursort.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import uk.co.tjcdeveloper.opencoloursort.data.Progress
import uk.co.tjcdeveloper.opencoloursort.data.ProgressRepository
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

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val progressRepository = ProgressRepository(app)

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
        // Resume at the first unsolved level of the first incomplete pack.
        // One-shot: later progress changes must not yank the player around.
        viewModelScope.launch {
            val stored = progressRepository.progress.first()
            val (targetPack, targetLevel) = firstUnsolved(stored)
            if (uiState.moveCount == 0 &&
                (targetPack != pack.id || targetLevel != levelNumber)
            ) {
                loadLevel(targetPack, targetLevel)
            }
        }
    }

    private fun firstUnsolved(progress: Progress): Pair<Int, Int> {
        for (candidate in Packs.all.filter { !it.isHard }) {
            for (level in 1..candidate.levels.size) {
                if (!progress.isSolved(candidate.id, level)) return candidate.id to level
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
                if (engine.isSolved && !solveRecorded) {
                    solveRecorded = true
                    val packId = pack.id
                    val level = levelNumber
                    val moves = engine.moveCount
                    val unlocked = newlyUnlockedPacks(packId, level)
                    if (unlocked.isNotEmpty()) {
                        uiState = uiState.copy(newlyUnlockedPacks = unlocked)
                    }
                    viewModelScope.launch {
                        progressRepository.recordSolve(packId, level, moves)
                    }
                }
            }
            else -> {
                val newSelection = if (engine.board.tubes[index].isNotEmpty()) index else null
                uiState = snapshot().copy(selectedTube = newSelection)
            }
        }
    }

    /** Pack names this solve unlocks, empty when replaying a solved level. */
    private fun newlyUnlockedPacks(packId: Int, level: Int): List<String> {
        val current = progress.value
        if (current.isSolved(packId, level)) return emptyList()
        return Packs.newlyUnlocked(
            before = { current.solvedInPack(it) },
            after = { current.solvedInPack(it) + if (it == packId) 1 else 0 },
        ).map { it.name }
    }

    fun onUndo() {
        if (engine.undo()) uiState = snapshot()
    }

    fun onRestart() {
        engine.restart()
        solveRecorded = false
        uiState = snapshot()
    }

    fun onExtraTube() {
        if (engine.useExtraTube()) uiState = snapshot()
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
