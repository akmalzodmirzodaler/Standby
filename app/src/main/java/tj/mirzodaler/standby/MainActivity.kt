package tj.mirzodaler.standby

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import tj.mirzodaler.standby.data.CalendarRepo
import tj.mirzodaler.standby.data.Prefs
import tj.mirzodaler.standby.media.MediaAccess
import tj.mirzodaler.standby.ui.SettingsScreen
import tj.mirzodaler.standby.ui.StandbyScreen

/** Настройки + предпросмотр заставки. */
class MainActivity : ComponentActivity() {

    // Увеличивается при возврате из системных настроек → статусы перечитываются
    private var resumeTick by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        setContent {
            var preview by remember { mutableStateOf(false) }
            var calendarTick by remember { mutableIntStateOf(0) }
            val calendarLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { calendarTick++ }

            // Предпросмотр — в горизонтальной ориентации, как на подставке
            LaunchedEffect(preview) {
                requestedOrientation = if (preview) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }

            if (preview) {
                BackHandler { preview = false }
                StandbyScreen(onExit = { preview = false })
            } else {
                val tick = resumeTick + calendarTick
                SettingsScreen(
                    prefs = prefs,
                    calendarOk = tick.let { CalendarRepo.hasPermission(this) },
                    mediaOk = tick.let { MediaAccess.isGranted(this) },
                    onCalendar = { calendarLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    onMedia = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onDreamSettings = { startActivity(Intent(Settings.ACTION_DREAM_SETTINGS)) },
                    onPreview = { prefs.previewed = true; preview = true }
                )
            }
        }
    }
}
