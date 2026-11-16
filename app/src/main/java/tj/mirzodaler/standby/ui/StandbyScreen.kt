package tj.mirzodaler.standby.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tj.mirzodaler.standby.data.*
import tj.mirzodaler.standby.media.MediaAccess
import tj.mirzodaler.standby.media.MediaWatcher
import tj.mirzodaler.standby.media.NowPlaying
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private val RU = Locale("ru")

/** Палитра: днём — белый/оранжевый, ночью (22:00–07:00) — тусклый красный. */
private data class Palette(val primary: Color, val secondary: Color, val card: Color, val accent: Color)

private val DayPalette = Palette(Color.White, Color(0xFF9A9AA0), Color(0xFF1C1C1E), Color(0xFFFF9500))
private val NightPalette = Palette(Color(0xFFB3261E), Color(0xFF6E1A14), Color(0xFF120404), Color(0xFFB3261E))

private const val MIN = 60_000L

/**
 * Экраны (свайп влево/вправо):
 *  0 — большие часы
 *  1 — дашборд: часы · календарь · погода + курсы
 *  2 — музыка
 * Долгий тап в любом месте — выход.
 */
@Composable
fun StandbyScreen(onExit: () -> Unit) {
    val ctx = LocalContext.current
    val now by rememberNow()
    val shift by rememberBurnInShift()
    val battery by rememberBattery()
    val music by rememberNowPlaying()

    val weather by rememberPolled(30 * MIN) { WeatherRepo.load() }
    val rates by rememberPolled(60 * MIN) { RatesRepo.load() }
    val events by rememberPolled(5 * MIN) { CalendarRepo.upcoming(ctx) }

    val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
    val p = if (hour >= 22 || hour < 7) NightPalette else DayPalette

    val pager = rememberPagerState(pageCount = { 3 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onExit() }) }
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shift.first, y = shift.second) // защита от выгорания AMOLED
        ) { page ->
            when (page) {
                0 -> ClockPage(now, p, battery)
                1 -> DashboardPage(now, p, events, weather, rates)
                else -> MusicPage(music, MediaAccess.isGranted(ctx), p)
            }
        }
    }
}

// ───────────────────────── Экран 0: часы ─────────────────────────

@Composable
private fun ClockPage(now: Date, p: Palette, battery: Pair<Int, Boolean>) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val clockSize = with(LocalDensity.current) { (maxHeight * 0.55f).toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SimpleDateFormat("HH:mm", RU).format(now),
                color = p.primary, fontSize = clockSize, fontWeight = FontWeight.Bold,
                lineHeight = clockSize
            )
            Text(dateLabel(now), color = p.secondary, fontSize = 22.sp)
        }
        val (level, charging) = battery
        Text(
            (if (charging) "⚡ " else "") + "$level%",
            color = p.secondary, fontSize = 16.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
        )
    }
}

// ───────────────────────── Экран 1: дашборд ─────────────────────────

@Composable
private fun DashboardPage(
    now: Date, p: Palette,
    events: List<CalEvent>?, weather: Weather?, rates: Rates?
) {
    Row(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.weight(1f).fillMaxHeight(), p) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val size = with(LocalDensity.current) { (maxHeight * 0.36f).toSp() }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        SimpleDateFormat("HH:mm", RU).format(now),
                        color = p.primary, fontSize = size, fontWeight = FontWeight.Bold, lineHeight = size
                    )
                    Text(dateLabel(now), color = p.secondary, fontSize = 16.sp)
                }
            }
        }
        Card(Modifier.weight(1.15f).fillMaxHeight(), p) {
            CalendarWidget(events, p)
        }
        Column(
            Modifier.weight(0.9f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.weight(1f).fillMaxWidth(), p) { WeatherWidget(weather, p) }
            Card(Modifier.weight(1f).fillMaxWidth(), p) { RatesWidget(rates, p) }
        }
    }
}

@Composable
private fun CalendarWidget(events: List<CalEvent>?, p: Palette) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Календарь", color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        when {
            events == null -> Hint("Дай доступ к календарю в приложении Standby", p)
            events.isEmpty() -> Hint("Ближайшие 2 дня свободны", p)
            else -> events.forEach { e ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(width = 4.dp, height = 34.dp)
                            .background(Color(e.color).copy(alpha = 1f), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            e.title, color = p.primary, fontSize = 17.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(eventTime(e), color = p.secondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherWidget(w: Weather?, p: Palette) {
    Column(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Худжанд", color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(w?.let { "${it.tempC}°" } ?: "…", color = p.primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(w?.label ?: "загрузка", color = p.secondary, fontSize = 14.sp)
    }
}

@Composable
private fun RatesWidget(r: Rates?, p: Palette) {
    Column(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text("Курс, сомони", color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (r == null) {
            Text("загрузка", color = p.secondary, fontSize = 14.sp)
        } else {
            RateRow("USD", r.usd, p)
            RateRow("EUR", r.eur, p)
            RateRow("100 RUB", r.rub100, p)
        }
    }
}

@Composable
private fun RateRow(code: String, value: Double, p: Palette) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(code, color = p.secondary, fontSize = 15.sp)
        Text(String.format(RU, "%.2f", value), color = p.primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ───────────────────────── Экран 2: музыка ─────────────────────────

@Composable
private fun MusicPage(np: NowPlaying?, granted: Boolean, p: Palette) {
    if (np == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Hint(
                if (granted) "Ничего не играет" else "Дай доступ к уведомлениям в приложении Standby,\nчтобы видеть музыку",
                p
            )
        }
        return
    }
    // Тикаем раз в секунду для прогресс-бара
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(np.playing) { while (np.playing) { delay(1000); tick++ } }

    Row(
        Modifier.fillMaxSize().padding(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Обложка
        Box(
            Modifier.fillMaxHeight().aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(p.card),
            contentAlignment = Alignment.Center
        ) {
            if (np.art != null) {
                Image(
                    np.art.asImageBitmap(), contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("♪", color = p.secondary, fontSize = 64.sp)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(np.app, color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                np.title, color = p.primary, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(np.artist, color = p.secondary, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

            if (np.durationMs > 0) {
                val pos = tick.let { np.livePosition() } // чтение tick → перерисовка раз в секунду
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).background(p.card, CircleShape)) {
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth((pos.toFloat() / np.durationMs).coerceIn(0f, 1f))
                            .background(p.primary, CircleShape)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(mmss(pos), color = p.secondary, fontSize = 13.sp)
                    Text("-" + mmss(np.durationMs - pos), color = p.secondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                ControlButton("⏮", 52.dp, p) { np.previous() }
                ControlButton(if (np.playing) "⏸" else "▶", 68.dp, p) { np.playPause() }
                ControlButton("⏭", 52.dp, p) { np.next() }
            }
        }
    }
}

@Composable
private fun ControlButton(symbol: String, size: Dp, p: Palette, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(p.card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = p.primary, fontSize = (size.value * 0.4f).sp)
    }
}

// ───────────────────────── Общие элементы ─────────────────────────

@Composable
private fun Card(modifier: Modifier, p: Palette, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.background(p.card, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun Hint(text: String, p: Palette) {
    Text(text, color = p.secondary, fontSize = 16.sp)
}

private fun dateLabel(d: Date): String =
    SimpleDateFormat("EEEE, d MMMM", RU).format(d).replaceFirstChar { it.titlecase(RU) }

private fun eventTime(e: CalEvent): String {
    val today = Calendar.getInstance()
    val day = Calendar.getInstance().apply { timeInMillis = e.begin }
    val sameDay = today.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    val dayLabel = if (sameDay) "Сегодня" else "Завтра"
    if (e.allDay) return "$dayLabel · весь день"
    val f = SimpleDateFormat("HH:mm", RU)
    return "$dayLabel · ${f.format(Date(e.begin))}–${f.format(Date(e.end))}"
}

private fun mmss(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(s / 60, s % 60)
}

// ───────────────────────── Источники состояния ─────────────────────────

/** Периодически вызывает loader; null-результат не затирает последнее удачное значение. */
@Composable
private fun <T> rememberPolled(periodMs: Long, loader: suspend () -> T?): State<T?> {
    val state = remember { mutableStateOf<T?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            loader()?.let { state.value = it }
            delay(periodMs)
        }
    }
    return state
}

@Composable
private fun rememberNowPlaying(): State<NowPlaying?> {
    val ctx = LocalContext.current.applicationContext
    val state = remember { mutableStateOf<NowPlaying?>(null) }
    DisposableEffect(ctx) {
        val watcher = MediaWatcher(ctx) { state.value = it }
        watcher.start()
        onDispose { watcher.stop() }
    }
    return state
}

@Composable
private fun rememberNow(): State<Date> {
    val state = remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            state.value = Date()
        }
    }
    return state
}

@Composable
private fun rememberBurnInShift(): State<Pair<Dp, Dp>> {
    val state = remember { mutableStateOf(0.dp to 0.dp) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(MIN)
            state.value = Random.nextInt(-8, 9).dp to Random.nextInt(-8, 9).dp
        }
    }
    return state
}

@Composable
private fun rememberBattery(): State<Pair<Int, Boolean>> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(0 to false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                i ?: return
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                state.value = (level * 100 / scale) to charging
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { receiver.onReceive(context, it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
