package tj.mirzodaler.standby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tj.mirzodaler.standby.data.*
import java.text.SimpleDateFormat
import java.util.Date

/** Настройки (портрет 384×832): 3 шага запуска, стиль часов, параметры. */
@Composable
fun SettingsScreen(
    prefs: Prefs,
    calendarOk: Boolean,
    mediaOk: Boolean,
    onCalendar: () -> Unit,
    onMedia: () -> Unit,
    onDreamSettings: () -> Unit,
    onPreview: () -> Unit
) {
    val c = DayColors
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { delay(1000 - System.currentTimeMillis() % 1000); now = Date() } }

    var style by remember { mutableStateOf(prefs.clockStyle) }
    var city by remember { mutableIntStateOf(prefs.cityIndex) }
    var night by remember { mutableIntStateOf(prefs.nightPreset) }
    var seconds by remember { mutableStateOf(prefs.showSeconds) }
    var burnIn by remember { mutableStateOf(prefs.burnInShift) }

    val step1 = calendarOk && mediaOk
    val progress = (listOf(step1, prefs.previewed).count { it } + 1).coerceAtMost(3)

    CompositionLocalProvider(LocalColors provides c) {
        Column(
            Modifier.fillMaxSize().background(Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Шапка
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(c.surface, CircleShape).border(1.dp, c.outline, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Ic.MOON, c.accent, 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    T("Standby", 22.sp, weight = FontWeight.Bold)
                    T("Galaxy S25+ · One UI 9", 12.sp, c.secondary, mono = true)
                }
            }

            // Прогресс
            StandbyCard(Modifier.fillMaxWidth(), padding = 16) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(c.accent, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    T("Быстрый старт за 3 шага", 14.sp, modifier = Modifier.weight(1f))
                    T("Шаг $progress из 3", 13.sp, c.secondary, mono = true)
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).background(c.surfaceHigh, CircleShape)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress / 3f).background(c.primary, CircleShape))
                }
                Spacer(Modifier.height(10.dp))
                T("Дай доступ, включи заставку и проверь, как Standby выглядит на AMOLED.", 13.sp, c.secondary, maxLines = 3)
            }

            // 1. Разрешения
            StepHeader(1, "Разрешения", "Система Android")
            StandbyCard(Modifier.fillMaxWidth(), padding = 16) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(Ic.CALENDAR)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        T("Календарь", 16.sp, weight = FontWeight.Medium)
                        T("Ближайшие события на дашборде", 12.sp, c.secondary)
                    }
                    if (calendarOk) GrantedChip() else SmallButton("Дать", onCalendar)
                }
            }
            StandbyCard(Modifier.fillMaxWidth(), padding = 16) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(Ic.MUSIC, if (mediaOk) c.secondary else c.accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        T("Музыка и плеер", 16.sp, weight = FontWeight.Medium)
                        T("Доступ к медиасессиям", 12.sp, c.secondary)
                    }
                    if (mediaOk) GrantedChip()
                }
                if (!mediaOk) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().background(c.surfaceHigh, RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Ic.SHIELD, c.accent, 16.dp)
                        Spacer(Modifier.width(8.dp))
                        T(
                            "Android называет это «доступом к уведомлениям». Уведомления не читаются и не сохраняются — нужен только трек, обложка и кнопки.",
                            12.sp, c.secondary, maxLines = 4
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    BigButton("Предоставить доступ", onMedia)
                }
            }

            // 2. Запуск
            StepHeader(2, "Запуск заставки", "Samsung One UI 9")
            StandbyCard(Modifier.fillMaxWidth(), padding = 16) {
                T("Активация режима Standby", 16.sp, weight = FontWeight.Medium)
                T("Выбери Standby как заставку и «Во время зарядки».", 12.sp, c.secondary, maxLines = 2)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().background(c.surfaceHigh, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    T("Дисплей ›", 13.sp, c.secondary); T("Заставка ›", 13.sp, c.secondary)
                    T("Standby", 13.sp, weight = FontWeight.Bold, mono = true)
                }
                Spacer(Modifier.height(12.dp))
                BigButton("Настройки заставки", onDreamSettings, primary = false)
            }

            // 3. Предпросмотр
            StepHeader(3, "Предпросмотр", if (prefs.previewed) "Готово" else "AMOLED")
            StandbyCard(Modifier.fillMaxWidth(), padding = 16) {
                Row(
                    Modifier.fillMaxWidth().height(110.dp).background(Color.Black, RoundedCornerShape(18.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClockPreview(style, now, Modifier.weight(1.3f).fillMaxHeight())
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        T(SimpleDateFormat("EEE, d MMM", RU).format(now), 12.sp, c.secondary)
                        T(CITIES[city].name, 12.sp, c.accent)
                        T(style.title, 12.sp, c.secondary, mono = true)
                    }
                }
                Spacer(Modifier.height(12.dp))
                T("Запусти Standby без кабеля. Выход — «Назад» или долгий тап.", 12.sp, c.secondary, maxLines = 2)
                Spacer(Modifier.height(12.dp))
                BigButton("Запустить предпросмотр", onPreview)
            }

            // Стиль часов
            T("Циферблат", 20.sp, weight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            T("Ещё можно переключать двойным тапом по часам на заставке", 12.sp, c.secondary, maxLines = 2)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClockStyle.entries.forEach { s ->
                    val sel = s == style
                    Column(
                        Modifier.width(150.dp).clip(RoundedCornerShape(20.dp))
                            .background(c.surface)
                            .border(if (sel) 2.dp else 1.dp, if (sel) c.accent else c.outline, RoundedCornerShape(20.dp))
                            .clickable { style = s; prefs.clockStyle = s }
                            .padding(10.dp)
                    ) {
                        Box(Modifier.fillMaxWidth().height(70.dp).background(Color.Black, RoundedCornerShape(12.dp)).padding(6.dp), contentAlignment = Alignment.Center) {
                            ClockPreview(s, now, Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.height(8.dp))
                        T(s.title, 13.sp, if (sel) c.accent else c.primary, if (sel) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            // Параметры
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                T("Параметры заставки", 20.sp, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(Modifier.background(c.surfaceHigh, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    T("v0.3", 11.sp, c.secondary, mono = true)
                }
            }
            StandbyCard(Modifier.fillMaxWidth(), padding = 4) {
                SettingRow(Ic.PIN, "Город погоды", "${CITIES[city].name} · Open-Meteo") {
                    city = (city + 1) % CITIES.size; prefs.cityIndex = city
                }
                Divider()
                SettingRow(Ic.MOON, "Ночной режим", "Красный спектр, без синего", value = nightLabel(night)) {
                    night = (night + 1) % NIGHT_PRESETS.size; prefs.nightPreset = night
                }
                Divider()
                SettingRow(Ic.RATES, "Валютные котировки", "USD, EUR, 100 RUB → TJS")
                Divider()
                ToggleRow(Ic.TIMER, "Секунды", "На цифровых и аналоговых часах", seconds) {
                    seconds = it; prefs.showSeconds = it
                }
                Divider()
                ToggleRow(Ic.SHIELD, "Защита от выгорания", "Сдвиг ±8 dp каждые 2 мин", burnIn) {
                    burnIn = it; prefs.burnInShift = it
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
                T("v0.3 · DreamService · 832×384 dp", 11.sp, c.secondary, mono = true)
            }
        }
    }
}

@Composable
private fun StepHeader(n: Int, title: String, hint: String) {
    val c = LocalColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(Modifier.size(26.dp).border(1.dp, c.outline, CircleShape), contentAlignment = Alignment.Center) {
            T("$n", 13.sp, mono = true)
        }
        Spacer(Modifier.width(10.dp))
        T(title, 22.sp, weight = FontWeight.Bold, modifier = Modifier.weight(1f))
        T(hint, 12.sp, c.secondary)
    }
}

@Composable
private fun IconBox(ic: Ic, tint: Color? = null) {
    val c = LocalColors.current
    Box(Modifier.size(38.dp).background(c.surfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
        Icon(ic, tint ?: c.secondary, 18.dp)
    }
}

@Composable
private fun GrantedChip() {
    val c = LocalColors.current
    Row(
        Modifier.background(c.surfaceHigh, CircleShape).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(Ic.CHECK, c.accent, 14.dp)
        T("Разрешено", 12.sp)
    }
}

@Composable
private fun SmallButton(text: String, onClick: () -> Unit) {
    val c = LocalColors.current
    Box(Modifier.clip(CircleShape).background(c.primary).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 7.dp)) {
        T(text, 13.sp, c.onPrimaryButton, FontWeight.SemiBold)
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit, primary: Boolean = true) {
    val c = LocalColors.current
    Box(
        Modifier.fillMaxWidth().height(50.dp).clip(CircleShape)
            .background(if (primary) c.primary else c.surfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (primary) c.onPrimaryButton else c.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingRow(ic: Ic, title: String, sub: String, value: String? = null, onClick: (() -> Unit)? = null) {
    val c = LocalColors.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(ic)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            T(title, 15.sp, weight = FontWeight.Medium)
            T(sub, 12.sp, c.secondary, mono = true)
        }
        value?.let { T(it, 14.sp, c.accent, FontWeight.Bold, mono = true) }
        if (onClick != null) { Spacer(Modifier.width(6.dp)); T("›", 18.sp, c.secondary) }
    }
}

@Composable
private fun ToggleRow(ic: Ic, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBox(ic)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            T(title, 15.sp, weight = FontWeight.Medium)
            T(sub, 12.sp, c.secondary, mono = true)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black, checkedTrackColor = c.primary,
                uncheckedThumbColor = c.secondary, uncheckedTrackColor = c.surfaceHigh, uncheckedBorderColor = c.outline
            )
        )
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(LocalColors.current.outline))
}
