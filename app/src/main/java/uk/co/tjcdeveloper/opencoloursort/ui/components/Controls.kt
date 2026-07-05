package uk.co.tjcdeveloper.opencoloursort.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.Image
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.tjcdeveloper.opencoloursort.ui.theme.Accent
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme

/** Primary text button (h44, radius 6, accent bg) - e.g. Restart. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 44,
) {
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Accent.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Accent.onPrimary,
            ),
        )
    }
}

/** Secondary text button (dialog Replay style). */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 48,
) {
    val scheme = LocalScheme.current
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (scheme.isDark) scheme.border else scheme.borderSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.textPrimary,
            ),
        )
    }
}

/**
 * Game action button (Undo / extra vial): icon plus remaining-uses count.
 * Both buttons share the same treatment — accent when usable, muted scheme
 * colours when spent or unavailable — in both themes.
 */
@Composable
fun ActionButton(
    icon: (Color) -> ImageVector,
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 48,
) {
    val scheme = LocalScheme.current
    val bg = if (enabled) Accent.primary else scheme.raised
    val fg = if (enabled) Accent.onPrimary else scheme.lockedText
    Row(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Image(
            painter = rememberVectorPainter(icon(fg)),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        BasicText(
            count.toString(),
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = fg),
        )
    }
}

/** Undo arrow icon, redrawn from the handoff's inline SVG. */
fun undoIcon(tint: Color): ImageVector =
    ImageVector.Builder(
        name = "undo", defaultWidth = 22.dp, defaultHeight = 22.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(tint), pathFillType = PathFillType.NonZero) {
            moveTo(10f, 9f); verticalLineTo(5f)
            lineTo(3f, 12f); lineTo(10f, 19f); verticalLineTo(14.9f)
            curveTo(15f, 14.9f, 18.5f, 16.5f, 21f, 20f)
            curveTo(20f, 15f, 17f, 10f, 10f, 9f)
            close()
        }
    }.build()

/** Extra-tube icon (tube with a plus), redrawn from the handoff's inline SVG. */
fun extraTubeIcon(tint: Color): ImageVector =
    ImageVector.Builder(
        name = "extraTube", defaultWidth = 22.dp, defaultHeight = 22.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        // Tube mouth
        path(
            stroke = SolidColor(tint), strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(9f, 4f); horizontalLineTo(15f)
            moveTo(12f, 2.5f); verticalLineTo(5.5f)
        }
        // Tube body
        path(
            stroke = SolidColor(tint), strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(8.5f, 7.5f); verticalLineTo(16.5f)
            arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15.5f, 16.5f)
            verticalLineTo(7.5f)
        }
        // Plus
        path(
            stroke = SolidColor(tint), strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(16f, 14.5f); horizontalLineTo(21f)
            moveTo(18.5f, 12f); verticalLineTo(17f)
        }
    }.build()

/** Stat chip: LEVEL / MOVES card. Optionally tappable (LEVEL opens the map). */
@Composable
fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val scheme = LocalScheme.current
    val tappable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(scheme.chip)
            .then(tappable)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            label,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = scheme.chipLabel,
            ),
        )
        BasicText(
            value,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.chipValue,
            ),
        )
    }
}
