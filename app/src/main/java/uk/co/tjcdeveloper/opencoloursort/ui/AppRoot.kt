package uk.co.tjcdeveloper.opencoloursort.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import uk.co.tjcdeveloper.opencoloursort.BuildConfig
import uk.co.tjcdeveloper.opencoloursort.data.Settings
import uk.co.tjcdeveloper.opencoloursort.data.SettingsRepository
import uk.co.tjcdeveloper.opencoloursort.levels.Packs
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme
import kotlinx.coroutines.launch

private const val GITHUB_URL = "https://github.com/tjcdeveloper/open-colour-sort"

@Composable
fun AppRoot(
    viewModel: GameViewModel,
    settings: Settings,
    settingsRepository: SettingsRepository,
) {
    val scheme = LocalScheme.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val progress by viewModel.progress.collectAsState()
    val state = viewModel.uiState
    var menuOpen by remember { mutableStateOf(false) }

    // Haptic tick on every successful pour, if enabled.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.lastPour?.id) {
        if (state.lastPour != null && settings.haptics) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    when (viewModel.screen) {
        Screen.GAME -> Box {
            val dialogShowing = state.isSolved || state.isStuck
            Box(
                Modifier
                    .alpha(if (dialogShowing) 0.25f else 1f)
                    // Keep TalkBack focus off the dimmed board behind a dialog.
                    .then(if (dialogShowing) Modifier.clearAndSetSemantics {} else Modifier),
            ) {
                val pack = Packs.byId(state.packId)
                AdaptiveGameScreen(
                    state = state,
                    settings = settings,
                    packLabel = pack.name,
                    packSolved = progress.solvedInPack(pack.slug),
                    packTotal = pack.levels.size,
                    onTubeTapped = viewModel::onTubeTapped,
                    onRestart = viewModel::onRestart,
                    onUndo = viewModel::onUndo,
                    onExtraTube = viewModel::onExtraTube,
                    onOverflowMenu = { menuOpen = true },
                    onOpenLevels = { viewModel.screen = Screen.LEVELS },
                )
            }
            if (state.isSolved) {
                WinDialog(
                    levelLabel = state.levelLabel,
                    moves = state.moveCount,
                    undosUsed = state.undosUsed,
                    unlockedPacks = state.newlyUnlockedPacks,
                    onReplay = viewModel::onRestart,
                    onNextLevel = viewModel::nextLevel,
                )
            }
            if (state.isStuck) {
                StalemateDialog(
                    levelLabel = state.levelLabel,
                    moves = state.moveCount,
                    canAddVial = state.extraTubesRemaining > 0,
                    canUndo = state.canUndo,
                    onAddVial = viewModel::onExtraTube,
                    onUndo = viewModel::onUndo,
                    onRestart = viewModel::onRestart,
                )
            }
            if (menuOpen) {
                // Simple overflow menu: Levels / Settings.
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable { menuOpen = false },
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .safeDrawingPadding()
                            .padding(top = 60.dp, end = 16.dp)
                            .width(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.card),
                    ) {
                        MenuRow("Levels") {
                            menuOpen = false
                            viewModel.screen = Screen.LEVELS
                        }
                        MenuRow("Settings") {
                            menuOpen = false
                            viewModel.screen = Screen.SETTINGS
                        }
                    }
                }
            }
        }

        Screen.LEVELS -> {
            var packId by remember { mutableStateOf(state.packId) }
            val pack = Packs.byId(packId)
            // Debug-build testing aid: every pack and level is playable.
            val unlockAll = BuildConfig.UNLOCK_ALL_LEVELS
            val unlocked = unlockAll ||
                Packs.isUnlocked(pack) { progress.solvedInPack(Packs.byId(it).slug) }
            val firstUnsolved = (1..pack.levels.size)
                .firstOrNull { !progress.isSolved(pack.slug, it) }
            val chips = (1..pack.levels.size).map { n ->
                LevelChip(
                    number = n,
                    state = when {
                        progress.isSolved(pack.slug, n) -> LevelChipState.SOLVED
                        unlocked && (unlockAll || n == firstUnsolved) -> LevelChipState.CURRENT
                        else -> LevelChipState.LOCKED
                    },
                )
            }
            LevelSelectScreen(
                packName = pack.name + if (unlocked) "" else " (locked)",
                solvedCount = progress.solvedInPack(pack.slug),
                totalCount = pack.levels.size,
                levels = chips,
                onBack = { viewModel.screen = Screen.GAME },
                onLevelTapped = { n ->
                    val chip = chips[n - 1]
                    if (chip.state != LevelChipState.LOCKED) viewModel.loadLevel(pack.id, n)
                },
                packCount = Packs.all.size,
                packIndex = packId,
                onSwitchPack = { delta ->
                    packId = (packId + delta).mod(Packs.all.size)
                },
            )
        }

        Screen.SETTINGS -> SettingsScreen(
            settings = settings,
            versionName = BuildConfig.VERSION_NAME,
            onBack = { viewModel.screen = Screen.GAME },
            onThemeChange = { scope.launch { settingsRepository.setTheme(it) } },
            onColorblindChange = { scope.launch { settingsRepository.setColorblindSymbols(it) } },
            onHapticsChange = { scope.launch { settingsRepository.setHaptics(it) } },
            onPaletteChange = { scope.launch { settingsRepository.setPalette(it) } },
            onTubeRadiusChange = { scope.launch { settingsRepository.setTubeBottomRadius(it) } },
            onOpenGitHub = {
                context.startActivity(Intent(Intent.ACTION_VIEW, GITHUB_URL.toUri()))
            },
        )
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    val scheme = LocalScheme.current
    Box(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        BasicText(
            label,
            style = TextStyle(fontSize = 15.sp, color = scheme.textPrimary),
        )
    }
}
