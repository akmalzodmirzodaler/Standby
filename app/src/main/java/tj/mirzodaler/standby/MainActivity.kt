package tj.mirzodaler.standby

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tj.mirzodaler.standby.data.CalendarRepo
import tj.mirzodaler.standby.media.MediaAccess
import tj.mirzodaler.standby.ui.StandbyScreen

/** Экран настройки: разрешения, переход в настройки заставки и предпросмотр. */
class MainActivity : ComponentActivity() {

    // Увеличивается при возврате из системных настроек → статусы перечитываются
    private var resumeTick by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var preview by remember { mutableStateOf(false) }
            val tick = resumeTick
            var calendarTick by remember { mutableIntStateOf(0) }
            val calendarLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { calendarTick++ }

            if (preview) {
                BackHandler { preview = false }
                StandbyScreen(onExit = { preview = false })
            } else {
                val calendarOk = (tick + calendarTick).let { CalendarRepo.hasPermission(this) }
                val mediaOk = tick.let { MediaAccess.isGranted(this) }
                SetupScreen(
                    calendarOk = calendarOk,
                    mediaOk = mediaOk,
                    onCalendar = { calendarLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    onMedia = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                    onDreamSettings = { startActivity(Intent(Settings.ACTION_DREAM_SETTINGS)) },
                    onPreview = { preview = true }
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(
    calendarOk: Boolean,
    mediaOk: Boolean,
    onCalendar: () -> Unit,
    onMedia: () -> Unit,
    onDreamSettings: () -> Unit,
    onPreview: () -> Unit
) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Standby", fontSize = 32.sp, color = Color.White)

            Section("Разрешения")
            PermissionRow("Календарь", "Ближайшие события на дашборде", calendarOk, onCalendar)
            PermissionRow(
                "Музыка",
                "Доступ к уведомлениям — только чтобы видеть и переключать трек (Spotify и др.). Уведомления не читаются.",
                mediaOk, onMedia
            )

            Section("Запуск")
            Text(
                "1. «Настройки заставки» → выбери «Standby — часы и виджеты».\n" +
                    "2. «Когда запускать» → «Во время зарядки».\n" +
                    "3. Включи автоповорот и поставь телефон горизонтально.\n\n" +
                    "Свайп — следующий экран. Долгий тап — выход.",
                color = Color(0xFFBBBBBB), fontSize = 15.sp
            )
            Button(onClick = onDreamSettings) { Text("Настройки заставки") }
            OutlinedButton(onClick = onPreview) { Text("Предпросмотр") }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, color = Color(0xFFFF9500), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun PermissionRow(title: String, hint: String, granted: Boolean, onGrant: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 17.sp)
            Text(hint, color = Color(0xFF8E8E93), fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        if (granted) {
            Text("✓ Есть", color = Color(0xFF34C759), fontSize = 15.sp)
        } else {
            FilledTonalButton(onClick = onGrant) { Text("Дать") }
        }
    }
}
