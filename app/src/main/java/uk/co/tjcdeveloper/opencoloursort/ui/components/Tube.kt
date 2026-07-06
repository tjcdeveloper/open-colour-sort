package uk.co.tjcdeveloper.opencoloursort.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.tjcdeveloper.opencoloursort.data.PaletteMode
import uk.co.tjcdeveloper.opencoloursort.game.GameColour
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme

/** Size spec for a tube; the handoff defines several per screen. */
data class TubeSpec(
    val width: Dp,
    val height: Dp,
    val segmentHeight: Dp,
    val borderWidth: Dp = 3.dp,
    val topRadius: Dp = 8.dp,
    val symbolSize: Int = 18,
)

val ClassicCoverSpec = TubeSpec(56.dp, 190.dp, 42.dp)
val ClassicInnerSpec = TubeSpec(64.dp, 220.dp, 49.dp, topRadius = 10.dp, symbolSize = 20)
val HardCoverSpec = TubeSpec(64.dp, 294.dp, 24.dp, symbolSize = 12)
val HardInnerSpec = TubeSpec(52.dp, 270.dp, 22.dp, symbolSize = 11)

/**
 * Classic tube: 3px outline, flat colour segments stacked from the bottom,
 * optional colorblind symbol per segment. Selecting lifts the tube slightly.
 *
 * [segments] is bottom -> top, as in [uk.co.tjcdeveloper.opencoloursort.game.Board].
 */
@Composable
fun Tube(
    segments: List<GameColour>,
    spec: TubeSpec,
    bottomRadius: Dp,
    palette: PaletteMode,
    showSymbols: Boolean,
    selected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val scheme = LocalScheme.current
    val lift by animateDpAsState(
        targetValue = if (selected) (-10).dp else 0.dp,
        animationSpec = tween(120),
        label = "tubeLift",
    )
    val shape = RoundedCornerShape(
        spec.topRadius, spec.topRadius, bottomRadius, bottomRadius,
    )
    val borderColour = if (selected) uk.co.tjcdeveloper.opencoloursort.ui.theme.Accent.primary else scheme.border
    Column(
        modifier = modifier
            .offset(y = lift)
            .width(spec.width)
            .height(spec.height)
            .clip(shape)
            .border(spec.borderWidth, borderColour, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onTap,
            )
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                if (selected) stateDescription = "selected"
            },
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Liquid level changes animate as the stack grows/shrinks (~300ms,
        // the handoff's 250-350ms pour timing).
        Column(Modifier.animateContentSize(tween(300))) {
            // Render top -> bottom
            segments.asReversed().forEach { colour ->
                Segment(colour, spec, palette, showSymbols)
            }
        }
    }
}

@Composable
private fun Segment(
    colour: GameColour,
    spec: TubeSpec,
    palette: PaletteMode,
    showSymbols: Boolean,
) {
    val fill = Color(if (palette == PaletteMode.SOFT) colour.soft else colour.vivid)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(spec.segmentHeight)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        if (showSymbols) {
            BasicText(
                colour.symbol,
                style = TextStyle(
                    fontSize = spec.symbolSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0x73000000), // rgba(0,0,0,0.45) per handoff
                ),
            )
        }
    }
}
