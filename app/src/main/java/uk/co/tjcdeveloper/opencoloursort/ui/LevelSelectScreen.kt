package uk.co.tjcdeveloper.opencoloursort.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.tjcdeveloper.opencoloursort.ui.theme.Accent
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme

enum class LevelChipState { SOLVED, CURRENT, LOCKED }

data class LevelChip(val number: Int, val state: LevelChipState)

/**
 * Level select (handoff 1e): back + title, pack card with progress,
 * 4-column grid of level chips (solved / current / locked).
 */
@Composable
fun LevelSelectScreen(
    packName: String,
    solvedCount: Int,
    totalCount: Int,
    levels: List<LevelChip>,
    onBack: () -> Unit,
    onLevelTapped: (Int) -> Unit,
    packCount: Int = 1,
    packIndex: Int = 0,
    onSwitchPack: (Int) -> Unit = {},
) {
    val scheme = LocalScheme.current
    val percent = if (totalCount == 0) 0 else solvedCount * 100 / totalCount
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.window)
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack, role = Role.Button)
                    .semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) {
                BasicText("←", style = TextStyle(fontSize = 22.sp, color = scheme.textPrimary))
            }
            BasicText(
                "Levels",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = scheme.textPrimary),
            )
        }

        // Pack card, with chevrons to move between packs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.card)
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (packCount > 1) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = { onSwitchPack(-1) }, role = Role.Button)
                        .semantics { contentDescription = "Previous pack" },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText("‹", style = TextStyle(fontSize = 22.sp, color = scheme.textMuted))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                BasicText(
                    packName,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary),
                )
                BasicText(
                    "$solvedCount of $totalCount solved",
                    modifier = Modifier.padding(top = 2.dp),
                    style = TextStyle(fontSize = 13.sp, color = scheme.textMuted),
                )
            }
            BasicText(
                "$percent%",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scheme.textBright),
            )
            if (packCount > 1) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(onClick = { onSwitchPack(1) }, role = Role.Button)
                        .semantics { contentDescription = "Next pack" },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText("›", style = TextStyle(fontSize = 22.sp, color = scheme.textMuted))
                }
            }
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(scheme.chip),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(percent / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Accent.primary),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(levels, key = { it.number }) { level ->
                LevelChipView(level, onTap = { onLevelTapped(level.number) })
            }
        }

        BasicText(
            "Free & open source. No ads, no purchases.",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            style = TextStyle(
                fontSize = 13.sp,
                color = scheme.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun LevelChipView(level: LevelChip, onTap: () -> Unit) {
    val scheme = LocalScheme.current
    val shape = RoundedCornerShape(8.dp)
    val spokenState = when (level.state) {
        LevelChipState.SOLVED -> "solved"
        LevelChipState.CURRENT -> "ready to play"
        LevelChipState.LOCKED -> "locked"
    }
    val base = Modifier
        .height(72.dp)
        .clip(shape)
        .semantics { contentDescription = "Level ${level.number}, $spokenState" }
    val decorated = when (level.state) {
        LevelChipState.SOLVED -> base
            .background(scheme.chip)
            .clickable(onClick = onTap, role = Role.Button)
        LevelChipState.CURRENT -> base
            .background(Accent.primary)
            .clickable(onClick = onTap, role = Role.Button)
        LevelChipState.LOCKED -> {
            val dash = scheme.border
            base.drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    ),
                )
            }
        }
    }
    val numberColour: Color
    val subColour: Color
    val sub: String
    when (level.state) {
        LevelChipState.SOLVED -> {
            numberColour = scheme.textMuted; subColour = Accent.primary; sub = "✓"
        }
        LevelChipState.CURRENT -> {
            numberColour = Accent.onPrimary; subColour = Color(0xFFEEE4DA); sub = "play"
        }
        LevelChipState.LOCKED -> {
            numberColour = scheme.lockedText; subColour = scheme.lockedText; sub = ""
        }
    }
    Column(
        modifier = decorated,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BasicText(
            level.number.toString(),
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = numberColour),
        )
        if (sub.isNotEmpty()) {
            BasicText(
                sub,
                modifier = Modifier.padding(top = 2.dp),
                style = TextStyle(fontSize = 12.sp, color = subColour),
            )
        }
    }
}
