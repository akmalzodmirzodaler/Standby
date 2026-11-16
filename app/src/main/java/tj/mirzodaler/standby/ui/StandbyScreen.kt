package tj.mirzodaler.standby.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

private val RU = Locale("ru")

/** Палитра: днём — белый/оранжевый, ночью (22:00–07:00) — тусклый красный, чтобы не слепило. */
private data class Palette(val primary: Color, val secondary: Color, val card: Color)

private val DayPalette = Palette(Color.White, Color(0xFFFF9500), Color(0xFF1C1C1E))
private val NightPalette = Palette(Color(0xFFB3261E), Color(0xFF7A1A14), Color(0xFF120404))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandbyScreen(onExit: () -> Unit) {
    val now by rememberNow()
    val shift by rememberBurnInShift()
    val battery by rememberBattery()
    var weather by remember { mutableStateOf<Weather?>(null) }

    // Погода раз в 30 минут
    LaunchedEffect(Unit) {
        while (true) {
            WeatherRepo.load()?.let { weather = it }
            delay(30 * 60 * 1000L)
        }
    }

    val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
    val palette = if (hour >= 22 || hour < 7) NightPalette else DayPalette

    val pager = rememberPagerState(pageCount = { 2 })

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
                0 -> BigClockPage(now, palette)
                else -> WidgetsPage(now, palette, weather, battery)
            }
        }
    }
}

/** Экран 1: огромные часы на весь экран + дата. */
@Composable
private fun BigClockPage(now: Date, p: Palette) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val clockSize = with(LocalDensity.current) { (maxHeight * 0.55f).toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = SimpleDateFormat("HH:mm", RU).format(now),
                color = p.primary,
                fontSize = clockSize,
                fontWeight = FontWeight.Bold,
                lineHeight = clockSize
            )
            Text(
                text = dateLabel(now),
                color = p.secondary,
                fontSize = 22.sp
            )
        }
    }
}

/** Экран 2: часы слева, карточки виджетов справа — как в StandBy на iPhone. */
@Composable
private fun WidgetsPage(now: Date, p: Palette, weather: Weather?, battery: Pair<Int, Boolean>) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(Modifier.weight(1f).fillMaxHeight(), p) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val size = with(LocalDensity.current) { (maxHeight * 0.38f).toSp() }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        SimpleDateFormat("HH:mm", RU).format(now),
                        color = p.primary, fontSize = size, fontWeight = FontWeight.Bold,
                        lineHeight = size
                    )
                    Text(dateLabel(now), color = p.secondary, fontSize = 18.sp)
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.weight(1f).fillMaxWidth(), p) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Худжанд", color = p.secondary, fontSize = 16.sp)
                    Text(
                        weather?.let { "${it.tempC}°" } ?: "…",
                        color = p.primary, fontSize = 48.sp, fontWeight = FontWeight.Bold
                    )
                    Text(weather?.label ?: "загрузка", color = p.secondary, fontSize = 16.sp)
                }
            }
            Card(Modifier.weight(1f).fillMaxWidth(), p) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val (level, charging) = battery
                    Text(
                        if (charging) "⚡ Заряжается" else "Батарея",
                        color = p.secondary, fontSize = 16.sp
                    )
                    Text(
                        "$level%",
                        color = p.primary, fontSize = 48.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier, p: Palette, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.background(p.card, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) { content() }
}

private fun dateLabel(d: Date): String =
    SimpleDateFormat("EEEE, d MMMM", RU).format(d).replaceFirstChar { it.titlecase(RU) }

/** Текущее время, обновляется ровно на смене секунды. */
@Composable
private fun rememberNow(): State<Date> {
    val state = remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            val ms = System.currentTimeMillis()
            delay(1000 - ms % 1000)
            state.value = Date()
        }
    }
    return state
}

/** Сдвиг всего контента на несколько dp раз в минуту — против выгорания пикселей. */
@Composable
private fun rememberBurnInShift(): State<Pair<Dp, Dp>> {
    val state = remember { mutableStateOf(0.dp to 0.dp) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            state.value = Random.nextInt(-8, 9).dp to Random.nextInt(-8, 9).dp
        }
    }
    return state
}

/** Уровень заряда и признак зарядки через системный sticky-broadcast. */
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
