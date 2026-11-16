package tj.mirzodaler.standby

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tj.mirzodaler.standby.ui.StandbyScreen

/** Экран настройки: инструкция, переход в настройки заставки и предпросмотр. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var preview by remember { mutableStateOf(false) }
            if (preview) {
                BackHandler { preview = false }
                StandbyScreen(onExit = { preview = false })
            } else {
                SetupScreen(
                    onOpenDreamSettings = {
                        startActivity(Intent(Settings.ACTION_DREAM_SETTINGS))
                    },
                    onPreview = { preview = true }
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(onOpenDreamSettings: () -> Unit, onPreview: () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Standby", fontSize = 32.sp, color = Color.White)
            Text(
                "1. Нажми «Настройки заставки».\n" +
                    "2. Выбери «Standby — часы и виджеты».\n" +
                    "3. «Когда запускать» → «Во время зарядки».\n" +
                    "4. Включи автоповорот и поставь телефон горизонтально.\n\n" +
                    "Свайп — следующий экран. Долгий тап — выход.",
                color = Color(0xFFBBBBBB),
                fontSize = 16.sp
            )
            Button(onClick = onOpenDreamSettings) { Text("Настройки заставки") }
            OutlinedButton(onClick = onPreview) { Text("Предпросмотр") }
        }
    }
}
