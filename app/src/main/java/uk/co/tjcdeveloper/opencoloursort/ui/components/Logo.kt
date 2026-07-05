package uk.co.tjcdeveloper.opencoloursort.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.tjcdeveloper.opencoloursort.ui.theme.Accent
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme

private val IconYellow = Color(0xFFE9C23A)
private val IconBlue = Color(0xFF3F7FD8)
private val IconRed = Color(0xFFDD4A44)
private val IconGreen = Color(0xFF57AB4A)
private val Hairline = Color(0x26000000)

/**
 * The 3b "before -> after" app icon, drawn in code. [size] is the icon edge;
 * all internal dimensions scale from the 128px reference.
 */
@Composable
fun AppIcon(size: Dp) {
    val s = size / 128f
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(Accent.primary),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(s * 14)) {
            // Left tube: mixed (top -> bottom: yellow, blue, red, green)
            IconTube(s, listOf(IconYellow, IconBlue, IconRed, IconGreen), hairlines = false)
            // Right tube: blocked mid-game (three green on top, bottom yellow)
            IconTube(s, listOf(IconGreen, IconGreen, IconGreen, IconYellow), hairlines = true)
        }
    }
}

@Composable
private fun IconTube(s: Dp, topToBottom: List<Color>, hairlines: Boolean) {
    // A 4px (at 128 reference) white ring with the liquid clipped inside.
    Box(
        modifier = Modifier
            .width(s * 32)
            .height(s * 80)
            .clip(RoundedCornerShape(s * 8, s * 8, s * 14, s * 14))
            .background(Color(0xF2FFFFFF)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(s * 24)
                .height(s * 72)
                .clip(RoundedCornerShape(s * 4, s * 4, s * 10, s * 10)),
        ) {
            topToBottom.forEachIndexed { i, colour ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(s * 24)
                        .background(colour),
                ) {
                    if (hairlines && i < 3) {
                        Box(
                            Modifier
                                .height(1.dp)
                                .width(s * 24)
                                .background(Hairline)
                                .align(Alignment.TopCenter),
                        )
                    }
                }
            }
        }
    }
}

/**
 * In-app header lockup: 42x42 icon + two-line wordmark.
 */
@Composable
fun HeaderLockup(iconSize: Dp = 42.dp) {
    val scheme = LocalScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(iconSize)
        Column {
            BasicText(
                "OPEN",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = scheme.textMuted,
                ),
            )
            BasicText(
                "Colour Sort",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.textPrimary,
                    lineHeight = 20.sp,
                ),
            )
        }
    }
}
