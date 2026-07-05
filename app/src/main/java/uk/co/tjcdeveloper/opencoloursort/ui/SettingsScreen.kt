package uk.co.tjcdeveloper.opencoloursort.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.tjcdeveloper.opencoloursort.data.Settings
import uk.co.tjcdeveloper.opencoloursort.data.ThemeMode
import uk.co.tjcdeveloper.opencoloursort.ui.theme.Accent
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme

private const val GITHUB_URL = "https://github.com/tjcdeveloper/open-colour-sort"

@Composable
fun SettingsScreen(
    settings: Settings,
    versionName: String,
    onBack: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onColorblindChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
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
                modifier = Modifier.size(44.dp).clickable(onClick = onBack),
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
            Column(Modifier.padding(16.dp)) {
                BasicText(
                    "Theme",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.textPrimary),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.raised)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ThemeSegment("Light", settings.theme == ThemeMode.LIGHT, Modifier.weight(1f)) {
                        onThemeChange(ThemeMode.LIGHT)
                    }
                    ThemeSegment("Dark", settings.theme == ThemeMode.DARK, Modifier.weight(1f)) {
                        onThemeChange(ThemeMode.DARK)
                    }
                    ThemeSegment("System", settings.theme == ThemeMode.SYSTEM, Modifier.weight(1f)) {
                        onThemeChange(ThemeMode.SYSTEM)
                    }
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
                    modifier = Modifier.padding(top = 6.dp).clickable(onClick = onOpenGitHub),
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
private fun ThemeSegment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = LocalScheme.current
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Accent.primary else scheme.raised)
            .clickable(onClick = onClick),
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
        modifier = Modifier.fillMaxWidth(),
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
                .clickable { onChange(!checked) }
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
