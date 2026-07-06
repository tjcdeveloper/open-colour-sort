package uk.co.tjcdeveloper.opencoloursort.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.tjcdeveloper.opencoloursort.data.PaletteMode
import uk.co.tjcdeveloper.opencoloursort.data.Settings
import uk.co.tjcdeveloper.opencoloursort.data.ThemeMode
import uk.co.tjcdeveloper.opencoloursort.ui.theme.Accent
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme
import kotlin.math.roundToInt

private const val GITHUB_URL = "https://github.com/tjcdeveloper/open-colour-sort"

@Composable
fun SettingsScreen(
    settings: Settings,
    versionName: String,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onColorblindChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onPaletteChange: (PaletteMode) -> Unit,
    onTubeRadiusChange: (Int) -> Unit,
    onOpenGitHub: () -> Unit,
) {
    val scheme = LocalScheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.window)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
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
                "Settings",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = scheme.textPrimary),
            )
        }

        SettingsSection("APPEARANCE") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    BasicText(
                        "Theme",
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary),
                    )
                    SegmentedRow {
                        Segment("Light", settings.theme == ThemeMode.LIGHT, Modifier.weight(1f)) {
                            onThemeChange(ThemeMode.LIGHT)
                        }
                        Segment("Dark", settings.theme == ThemeMode.DARK, Modifier.weight(1f)) {
                            onThemeChange(ThemeMode.DARK)
                        }
                        Segment("System", settings.theme == ThemeMode.SYSTEM, Modifier.weight(1f)) {
                            onThemeChange(ThemeMode.SYSTEM)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.raised))
                Column {
                    BasicText(
                        "Palette",
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary),
                    )
                    BasicText(
                        "Colour intensity of the liquids",
                        modifier = Modifier.padding(top = 2.dp),
                        style = TextStyle(fontSize = 13.sp, color = scheme.textMuted),
                    )
                    SegmentedRow {
                        Segment("Vivid", settings.palette == PaletteMode.VIVID, Modifier.weight(1f)) {
                            onPaletteChange(PaletteMode.VIVID)
                        }
                        Segment("Soft", settings.palette == PaletteMode.SOFT, Modifier.weight(1f)) {
                            onPaletteChange(PaletteMode.SOFT)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.raised))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BasicText(
                            "Tube bottom radius",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = scheme.textPrimary,
                            ),
                        )
                        BasicText(
                            "${settings.tubeBottomRadius}dp",
                            style = TextStyle(fontSize = 14.sp, color = scheme.textMuted),
                        )
                    }
                    BasicText(
                        "How rounded the tubes are",
                        modifier = Modifier.padding(top = 2.dp),
                        style = TextStyle(fontSize = 13.sp, color = scheme.textMuted),
                    )
                    RadiusSlider(
                        value = settings.tubeBottomRadius,
                        onChange = onTubeRadiusChange,
                    )
                }
            }
        }

        SettingsSection("ACCESSIBILITY") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ToggleRow(
                    title = "Colorblind symbols",
                    subtitle = "Show a shape on every colour",
                    checked = settings.colorblindSymbols,
                    onChange = onColorblindChange,
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(scheme.raised))
                ToggleRow(
                    title = "Haptics",
                    subtitle = "Vibrate on pour",
                    checked = settings.haptics,
                    onChange = onHapticsChange,
                )
            }
        }

        SettingsSection("ABOUT") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    "Open Colour Sort · v$versionName",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary),
                )
                BasicText(
                    "Free and open source. No ads, no purchases, no tracking.",
                    style = TextStyle(fontSize = 13.sp, color = scheme.textMuted, lineHeight = 19.sp),
                )
                BasicText(
                    "View source on GitHub →",
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable(onClick = onOpenGitHub, role = Role.Button),
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Accent.link),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(label: String, content: @Composable () -> Unit) {
    val scheme = LocalScheme.current
    Column {
        BasicText(
            label,
            modifier = Modifier.padding(bottom = 10.dp),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = scheme.textMuted,
            ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(scheme.card),
        ) {
            content()
        }
    }
}

@Composable
private fun SegmentedRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val scheme = LocalScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.raised)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = LocalScheme.current
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Accent.primary else scheme.raised)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Accent.onPrimary else scheme.textMuted,
            ),
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = LocalScheme.current
    Row(
        // The whole row toggles, giving a large target and Switch semantics.
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            BasicText(
                title,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary),
            )
            BasicText(
                subtitle,
                modifier = Modifier.padding(top = 2.dp),
                style = TextStyle(fontSize = 13.sp, color = scheme.textMuted),
            )
        }
        // 52x32 pill toggle
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(if (checked) Accent.primary else scheme.raised)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (checked) Accent.onPrimary else scheme.textMuted),
            )
        }
    }
}

/**
 * Drag/tap slider for the tube bottom radius, styled like the pack progress
 * bars (no Material). Values snap to whole dp inside the handoff's 4-28
 * range.
 */
@Composable
private fun RadiusSlider(value: Int, onChange: (Int) -> Unit) {
    val scheme = LocalScheme.current
    var trackWidthPx by remember { mutableIntStateOf(1) }
    // Drag position lives locally so the knob follows the finger instantly;
    // onChange fires only when the snapped value actually changes, keeping
    // DataStore writes to one per step instead of one per drag sample.
    var dragValue by remember { mutableStateOf<Int?>(null) }
    // pointerInput(Unit) closures outlive recompositions; read the latest
    // committed value through State so drag comparisons are never stale.
    val committedValue by rememberUpdatedState(value)
    val range = Settings.MAX_TUBE_RADIUS - Settings.MIN_TUBE_RADIUS
    fun valueAt(x: Float): Int =
        (Settings.MIN_TUBE_RADIUS + (x / trackWidthPx) * range).roundToInt()
            .coerceIn(Settings.MIN_TUBE_RADIUS, Settings.MAX_TUBE_RADIUS)

    val shown = dragValue ?: value
    val fraction = (shown - Settings.MIN_TUBE_RADIUS).toFloat() / range
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .onSizeChanged { trackWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) { detectTapGestures { offset -> onChange(valueAt(offset.x)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { dragValue = null },
                    onDragCancel = { dragValue = null },
                ) { change, _ ->
                    val target = valueAt(change.position.x)
                    if (target != (dragValue ?: committedValue)) onChange(target)
                    dragValue = target
                }
            }
            .semantics {
                contentDescription = "Tube bottom radius"
                stateDescription = "$shown dp"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = shown.toFloat(),
                    range = Settings.MIN_TUBE_RADIUS.toFloat()..Settings.MAX_TUBE_RADIUS.toFloat(),
                    steps = range - 1,
                )
                setProgress { target ->
                    onChange(target.roundToInt().coerceIn(Settings.MIN_TUBE_RADIUS, Settings.MAX_TUBE_RADIUS))
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(scheme.chip),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Accent.primary),
        )
        // Knob: positioned by fraction of the available width.
        Row(Modifier.fillMaxWidth()) {
            if (fraction > 0f) Box(Modifier.weight(fraction.coerceAtLeast(0.0001f)))
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Accent.onPrimary)
            )
            if (fraction < 1f) Box(Modifier.weight((1f - fraction).coerceAtLeast(0.0001f)))
        }
    }
}
