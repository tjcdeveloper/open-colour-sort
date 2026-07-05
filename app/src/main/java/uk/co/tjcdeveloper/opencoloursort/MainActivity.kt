package uk.co.tjcdeveloper.opencoloursort

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import uk.co.tjcdeveloper.opencoloursort.data.Settings
import uk.co.tjcdeveloper.opencoloursort.data.SettingsRepository
import uk.co.tjcdeveloper.opencoloursort.data.ThemeMode
import uk.co.tjcdeveloper.opencoloursort.ui.AppRoot
import uk.co.tjcdeveloper.opencoloursort.ui.GameViewModel
import uk.co.tjcdeveloper.opencoloursort.ui.theme.DarkScheme
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LightScheme
import uk.co.tjcdeveloper.opencoloursort.ui.theme.LocalScheme

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            val settings by remember { settingsRepository.settings }
                .collectAsState(initial = Settings())
            val dark = when (settings.theme) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CompositionLocalProvider(LocalScheme provides if (dark) DarkScheme else LightScheme) {
                AppRoot(
                    viewModel = gameViewModel,
                    settings = settings,
                    settingsRepository = settingsRepository,
                )
            }
        }
    }
}
