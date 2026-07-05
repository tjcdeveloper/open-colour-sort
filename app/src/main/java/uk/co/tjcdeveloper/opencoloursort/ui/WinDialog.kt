package uk.co.tjcdeveloper.opencoloursort.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.tjcdeveloper.opencoloursort.ui.components.PrimaryButton
import uk.co.tjcdeveloper.opencoloursort.ui.components.SecondaryButton
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme

/**
 * Level-complete dialog (handoff 1f): centered card over the dimmed board,
 * conic-gradient badge with a check, stats line, Replay / Next level.
 */
@Composable
fun WinDialog(
    levelLabel: String,
    moves: Int,
    undosUsed: Int,
    onReplay: () -> Unit,
    onNextLevel: () -> Unit,
) {
    val scheme = LocalScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.card)
                .border(1.dp, scheme.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 56px badge: red/yellow/blue/green quadrants with white check
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    // Compose sweep gradients start at 3 o'clock; the design's
                    // CSS conic starts at 12. Rotate to line the quadrants up.
                    .rotate(-90f)
                    .background(
                        Brush.sweepGradient(
                            0f to Color(0xFFDD4A44), 0.25f to Color(0xFFDD4A44),
                            0.25f to Color(0xFFE9C23A), 0.5f to Color(0xFFE9C23A),
                            0.5f to Color(0xFF3F7FD8), 0.75f to Color(0xFF3F7FD8),
                            0.75f to Color(0xFF57AB4A), 1f to Color(0xFF57AB4A),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    "✓",
                    modifier = Modifier.rotate(90f),
                    style = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )
            }
            BasicText(
                "Level $levelLabel solved!",
                modifier = Modifier.padding(top = 10.dp),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.textBright,
                ),
            )
            BasicText(
                if (undosUsed == 0) "$moves moves · no undos" else "$moves moves · $undosUsed undos",
                style = TextStyle(fontSize = 14.sp, color = scheme.textMuted),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryButton("Replay", onReplay, Modifier.weight(1f))
                PrimaryButton("Next level", onNextLevel, Modifier.weight(1f), height = 48)
            }
        }
    }
}

/**
 * Stalemate dialog, mirroring the win dialog's design: shown when no pour can
 * move a complete colour run and the extra tube is spent. Undo appears as the
 * secondary action while any undos are left to escape with.
 */
@Composable
fun StalemateDialog(
    levelLabel: String,
    moves: Int,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
) {
    val scheme = LocalScheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.card)
                .border(1.dp, scheme.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 56px badge: solid brand red with a white cross.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDD4A44)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    "✕",
                    style = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    ),
                )
            }
            BasicText(
                "No moves left!",
                modifier = Modifier.padding(top = 10.dp),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.textBright,
                ),
            )
            BasicText(
                "Level $levelLabel · $moves moves",
                style = TextStyle(fontSize = 14.sp, color = scheme.textMuted),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (canUndo) SecondaryButton("Undo", onUndo, Modifier.weight(1f))
                PrimaryButton("Restart", onRestart, Modifier.weight(1f), height = 48)
            }
        }
    }
}
