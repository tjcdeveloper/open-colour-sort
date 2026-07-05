package uk.co.tjcdeveloper.opencoloursort.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.tjcdeveloper.opencoloursort.data.Settings
import uk.co.tjcdeveloper.opencoloursort.ui.components.ClassicInnerSpec
import uk.co.tjcdeveloper.opencoloursort.ui.components.HardCoverSpec
import uk.co.tjcdeveloper.opencoloursort.ui.components.HardInnerSpec
import uk.co.tjcdeveloper.opencoloursort.ui.components.ActionButton
import uk.co.tjcdeveloper.opencoloursort.ui.components.HeaderLockup
import uk.co.tjcdeveloper.opencoloursort.ui.components.PrimaryButton
import uk.co.tjcdeveloper.opencoloursort.ui.components.StatChip
import uk.co.tjcdeveloper.opencoloursort.ui.components.Tube
import uk.co.tjcdeveloper.opencoloursort.ui.components.TubeSpec
import uk.co.tjcdeveloper.opencoloursort.ui.components.ClassicCoverSpec
import uk.co.tjcdeveloper.opencoloursort.ui.components.extraTubeIcon
import uk.co.tjcdeveloper.opencoloursort.ui.components.undoIcon
import uk.co.tjcdeveloper.opencoloursort.ui.theme.Accent
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme

/** True when the level uses hard-mode dimensions (capacity > 4). */
private val GameUiState.isHard: Boolean get() = board.capacity > 4

/**
 * Chooses the cover column layout or the unfolded side-rail layout (1h)
 * based on available width, and hard-mode tube sizes when capacity > 4.
 */
@Composable
fun AdaptiveGameScreen(
    state: GameUiState,
    settings: Settings,
    packLabel: String?,
    packSolved: Int,
    packTotal: Int,
    onTubeTapped: (Int) -> Unit,
    onRestart: () -> Unit,
    onUndo: () -> Unit,
    onExtraTube: () -> Unit,
    onOverflowMenu: () -> Unit,
    onOpenLevels: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            UnfoldedGameScreen(
                state, settings, packLabel, packSolved, packTotal,
                onTubeTapped, onRestart, onUndo, onExtraTube, onOverflowMenu, onOpenLevels,
            )
        } else {
            CoverGameScreen(
                state, settings,
                onTubeTapped, onRestart, onUndo, onExtraTube, onOverflowMenu, onOpenLevels,
            )
        }
    }
}

/** Cover layout (1a/1b/2a): column with header, chips, actions, board, footer. */
@Composable
private fun CoverGameScreen(
    state: GameUiState,
    settings: Settings,
    onTubeTapped: (Int) -> Unit,
    onRestart: () -> Unit,
    onUndo: () -> Unit,
    onExtraTube: () -> Unit,
    onOverflowMenu: () -> Unit,
    onOpenLevels: () -> Unit,
) {
    val scheme = LocalScheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.window)
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderLockup()
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(44.dp)
                    .clickable(onClick = onOverflowMenu),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    "⋮",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatChip("LEVEL", state.levelLabel, Modifier.weight(1f), onClick = onOpenLevels)
            StatChip("MOVES", state.moveCount.toString(), Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton("Restart", onRestart)
            Spacer(Modifier.weight(1f))
            ActionButton(
                icon = ::extraTubeIcon,
                count = state.extraTubesRemaining,
                enabled = state.extraTubesRemaining > 0,
                onClick = onExtraTube,
            )
            ActionButton(
                icon = ::undoIcon,
                count = state.undosRemaining,
                enabled = state.canUndo,
                onClick = onUndo,
            )
        }

        val spec = if (state.isHard) HardCoverSpec else ClassicCoverSpec
        val boardWidth = if (state.isHard) 300.dp else 340.dp
        val gap = if (state.isHard) 14.dp else 16.dp

        // Board area: hard boards are taller than the viewport and scroll
        // vertically behind top/bottom fade gradients (2a).
        Box(Modifier.fillMaxWidth().weight(1f).padding(top = 10.dp)) {
            val scroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll),
                contentAlignment = Alignment.Center,
            ) {
                BoardFlow(state, settings, spec, boardWidth, gap, onTubeTapped)
            }
            // Edge fades
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(scheme.window, scheme.window.copy(alpha = 0f)))),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(scheme.window.copy(alpha = 0f), scheme.window))),
            )
        }

        BasicText(
            "Free & open source. No ads, no purchases.",
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            style = TextStyle(fontSize = 13.sp, color = scheme.textMuted, textAlign = TextAlign.Center),
        )
    }
}

/** Unfolded layout (1h/2c): board fills left, 240dp control rail right. */
@Composable
private fun UnfoldedGameScreen(
    state: GameUiState,
    settings: Settings,
    packLabel: String?,
    packSolved: Int,
    packTotal: Int,
    onTubeTapped: (Int) -> Unit,
    onRestart: () -> Unit,
    onUndo: () -> Unit,
    onExtraTube: () -> Unit,
    onOverflowMenu: () -> Unit,
    onOpenLevels: () -> Unit,
) {
    val scheme = LocalScheme.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.window)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val spec = if (state.isHard) HardInnerSpec else ClassicInnerSpec
            val boardWidth = if (state.isHard) 500.dp else 440.dp
            val gap = if (state.isHard) 12.dp else 20.dp
            BoardFlow(state, settings, spec, boardWidth, gap, onTubeTapped)
            BasicText(
                "Free & open source. No ads, no purchases.",
                modifier = Modifier.padding(top = 16.dp),
                style = TextStyle(fontSize = 13.sp, color = scheme.textMuted, textAlign = TextAlign.Center),
            )
        }
        Column(
            modifier = Modifier.width(240.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderLockup(iconSize = 48.dp)
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(44.dp)
                        .clickable(onClick = onOverflowMenu),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        "⋮",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary),
                    )
                }
            }
            StatChip("LEVEL", state.levelLabel, Modifier.fillMaxWidth(), onClick = onOpenLevels)
            StatChip("MOVES", state.moveCount.toString(), Modifier.fillMaxWidth())
            PrimaryButton("Restart", onRestart, Modifier.fillMaxWidth(), height = 48)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    icon = ::extraTubeIcon,
                    count = state.extraTubesRemaining,
                    enabled = state.extraTubesRemaining > 0,
                    onClick = onExtraTube,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    icon = ::undoIcon,
                    count = state.undosRemaining,
                    enabled = state.canUndo,
                    onClick = onUndo,
                    modifier = Modifier.weight(1f),
                )
            }
            if (packLabel != null) {
                Column(Modifier.padding(top = 8.dp)) {
                    BasicText(
                        packLabel.uppercase(),
                        style = TextStyle(
                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, color = scheme.textMuted,
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(scheme.chip),
                    ) {
                        val fraction = if (packTotal == 0) 0f else packSolved.toFloat() / packTotal
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Accent.primary),
                        )
                    }
                    BasicText(
                        "$packSolved of $packTotal solved",
                        modifier = Modifier.padding(top = 6.dp),
                        style = TextStyle(fontSize = 13.sp, color = scheme.textMuted),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardFlow(
    state: GameUiState,
    settings: Settings,
    spec: TubeSpec,
    boardWidth: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp,
    onTubeTapped: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.widthIn(max = boardWidth),
        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        state.board.tubes.forEachIndexed { index, tube ->
            Tube(
                segments = tube,
                spec = spec,
                bottomRadius = settings.tubeBottomRadius.dp,
                palette = settings.palette,
                showSymbols = settings.colorblindSymbols,
                selected = state.selectedTube == index,
                onTap = { onTubeTapped(index) },
            )
        }
    }
}
