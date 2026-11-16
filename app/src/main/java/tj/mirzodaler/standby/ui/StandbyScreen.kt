package tj.mirzodaler.standby.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tj.mirzodaler.standby.data.*
import tj.mirzodaler.standby.media.MediaAccess
import tj.mirzodaler.standby.media.MediaWatcher
import tj.mirzodaler.standby.media.NowPlaying
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.random.Random

private const val MIN = 60_000L
private val TABS = listOf("Часы", "Дашборд", "Медиаплеер")

/**
 * Экраны (свайп): 0 — часы · 1 — дашборд · 2 — медиаплеер.
 * Тап — показать навигацию, двойной тап на часах — следующий стиль, долгий тап — выход.
 */
@Composable
fun StandbyScreen(onExit: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var style by remember { mutableStateOf(prefs.clockStyle) }
    val showSeconds = remember { prefs.showSeconds }
    val city = remember { prefs.city }

    val now by rememberNow()
    val shift by rememberBurnInShift(remember { prefs.burnInShift })
    val battery by rememberBattery()
    val music by rememberNowPlaying()
    val weather by rememberPolled(30 * MIN) { WeatherRepo.load(city) }
    val rates by rememberPolled(60 * MIN) { RatesRepo.load(ctx) }
    val agenda by rememberPolled(5 * MIN) { CalendarRepo.agenda(ctx) }
    val alarm by rememberPolled(MIN, keepLast = false) { AlarmRepo.next(ctx) }

    val hour = Calendar.getInstance().apply { time = now }.get(Calendar.HOUR_OF_DAY)
    val colors = if (prefs.isNight(hour)) NightColors else DayColors

    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    // Навигация (точки, вкладки) появляется после касания и гаснет через 4 с
    var chromeTick by remember { mutableIntStateOf(0) }
    var chrome by remember { mutableStateOf(true) }
    LaunchedEffect(chromeTick, pager.currentPage) { chrome = true; delay(4000); chrome = false }

    // Таймер сна живёт здесь, чтобы не сбрасываться при пролистывании
    var sleepAt by remember { mutableStateOf<Long?>(null) }
    val currentMusic by rememberUpdatedState(music)
    LaunchedEffect(sleepAt) {
        val at = sleepAt ?: return@LaunchedEffect
        delay((at - System.currentTimeMillis()).coerceAtLeast(0))
        currentMusic?.let { if (it.playing) it.pause() }
        sleepAt = null
    }

    CompositionLocalProvider(LocalColors provides colors) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { chromeTick++ },
                        onDoubleTap = {
                            if (pager.currentPage == 0) {
                                style = style.next(); prefs.clockStyle = style
                            }
                            chromeTick++
                        },
                        onLongPress = { onExit() }
                    )
                }
        ) {
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().offset(shift.first, shift.second)
            ) { page ->
                when (page) {
                    0 -> ClockPage(style, now, showSeconds, alarm, battery, weather, chrome, page)
                    1 -> DashboardPage(now, alarm, battery, agenda, weather, rates, city, chrome, page)
                    else -> MediaPage(music, MediaAccess.isGranted(ctx), alarm, battery, now, chrome, page, sleepAt) { sleepAt = it }
                }
            }
            AnimatedVisibility(
                chrome, enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
            ) {
                TabBar(pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
            }
        }
    }
}

// ═════════════════════════ Верхняя панель ═════════════════════════

@Composable
private fun TopBar(
    alarm: Long?, battery: BatteryInfo, now: Date, chrome: Boolean, page: Int,
    countdown: Boolean = false, extra: @Composable RowScope.() -> Unit = {}
) {
    val dotsAlpha by animateFloatAsState(if (chrome) 1f else 0f, label = "dots")
    Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { AlarmPill(alarm, now, countdown) }
        PageDots(3, page, Modifier.alpha(dotsAlpha))
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatteryPill(battery)
            extra()
        }
    }
}

@Composable
private fun TabBar(current: Int, onSelect: (Int) -> Unit) {
    val c = LocalColors.current
    Row(
        Modifier.background(c.surface, CircleShape).border(1.dp, c.outline, CircleShape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TABS.forEachIndexed { i, title ->
            val sel = i == current
            Box(
                Modifier.clip(CircleShape)
                    .background(if (sel) c.primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                T(title, 14.sp, if (sel) c.onPrimaryButton else c.secondary, if (sel) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

// ═════════════════════════ Экран 1: часы ═════════════════════════

@Composable
private fun ClockPage(
    style: ClockStyle, now: Date, showSeconds: Boolean, alarm: Long?,
    battery: BatteryInfo, weather: Weather?, chrome: Boolean, page: Int
) {
    val c = LocalColors.current
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
        TopBar(alarm, battery, now, chrome, page)
        if (style.isAnalog) {
            // Аналог: циферблат слева, крупная дата и погода справа
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ClockFace(style, now, showSeconds, Modifier.fillMaxHeight())
                Spacer(Modifier.width(48.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    T(SimpleDateFormat("HH:mm", RU).format(now), 52.sp, weight = FontWeight.Bold, mono = true)
                    T(weekday(now), 26.sp, weight = FontWeight.SemiBold)
                    T(SimpleDateFormat("d MMMM", RU).format(now), 20.sp, c.secondary)
                    weather?.let { w ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WeatherIcon(w.kind, w.isDay, c.accent, c.secondary, 24.dp)
                            T("${w.tempC}°C ${w.label}", 20.sp, c.secondary)
                        }
                    }
                }
            }
        } else {
            ClockFace(style, now, showSeconds, Modifier.weight(1f).fillMaxWidth())
            InfoLine(now, weather, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp))
        }
    }
}

@Composable
private fun InfoLine(now: Date, weather: Weather?, modifier: Modifier) {
    val c = LocalColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        T(dateLong(now), 20.sp)
        weather?.let { w ->
            Box(Modifier.size(4.dp).background(c.secondary, CircleShape))
            WeatherIcon(w.kind, w.isDay, c.accent, c.secondary, 22.dp)
            T("${w.tempC}°C ${w.label}", 20.sp, c.secondary)
        }
    }
}

// ═════════════════════════ Экран 2: дашборд ═════════════════════════

@Composable
private fun DashboardPage(
    now: Date, alarm: Long?, battery: BatteryInfo, agenda: Agenda?, weather: Weather?,
    rates: Rates?, city: City, chrome: Boolean, page: Int
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        TopBar(alarm, battery, now, chrome, page, countdown = true)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeCard(now, city, Modifier.weight(0.92f).fillMaxHeight())
            CalendarCard(agenda, now, Modifier.weight(1.35f).fillMaxHeight())
            Column(Modifier.weight(1.08f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherCard(weather, city, now, Modifier.weight(1f).fillMaxWidth())
                RatesCard(rates, Modifier.weight(1.1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TimeCard(now: Date, city: City, modifier: Modifier) {
    val c = LocalColors.current
    StandbyCard(modifier, padding = 18) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(c.accent, CircleShape))
            Spacer(Modifier.width(6.dp))
            Label("Время и дата")
            Spacer(Modifier.weight(1f))
            Label(city.utc)
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            T(SimpleDateFormat("HH:mm", RU).format(now), 42.sp, weight = FontWeight.Bold, mono = true)
            T(SimpleDateFormat(":ss", RU).format(now), 17.sp, c.secondary, mono = true, modifier = Modifier.padding(bottom = 6.dp))
        }
        T(weekday(now), 20.sp, weight = FontWeight.Medium)
        T(dayMonthYear(now), 14.sp, c.secondary)
        Spacer(Modifier.weight(1f))
        Pill(bg = c.surfaceHigh) {
            Icon(Ic.PIN, c.secondary, 14.dp)
            T(city.name, 13.sp)
            if (c.isNight) {
                Icon(Ic.MOON, c.accent, 12.dp)
                T("Ночь", 13.sp, c.accent)
            }
        }
    }
}

@Composable
private fun CalendarCard(agenda: Agenda?, now: Date, modifier: Modifier) {
    val c = LocalColors.current
    StandbyCard(modifier, padding = 16) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ic.CALENDAR, c.accent, 18.dp)
            Spacer(Modifier.width(8.dp))
            T("Календарь", 16.sp, weight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (agenda != null && agenda.todayCount > 0) {
                Box(
                    Modifier.border(1.dp, c.accent.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) { T(eventsToday(agenda.todayCount), 12.sp, c.accent) }
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            agenda == null -> EmptyState("Нет доступа к календарю", "Standby → Разрешения → Календарь")
            agenda.timed.isEmpty() && agenda.allDay == null -> EmptyState("Ближайшие 2 дня свободны", "Можно планировать")
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    agenda.timed.forEach { EventRow(it, now) }
                }
                Spacer(Modifier.weight(1f))
                agenda.allDay?.let { e ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(c.accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).background(c.accent, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        T(e.title, 14.sp, modifier = Modifier.weight(1f))
                        T("ВЕСЬ ДЕНЬ", 11.sp, c.accent, FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(e: CalEvent, now: Date) {
    val c = LocalColors.current
    val barColor = if (c.isNight) c.primary else Color(e.color).copy(alpha = 1f)
    val ongoing = e.begin <= now.time
    val whenText = when {
        ongoing -> "Идёт сейчас"
        isToday(e.begin) -> "Через " + duration(e.begin - now.time)
        else -> "Завтра"
    }
    val sub = listOf(e.calendar, e.location).filter { it.isNotBlank() }.firstOrNull()
    Row(
        Modifier.fillMaxWidth().background(c.surfaceHigh, RoundedCornerShape(14.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(34.dp).background(barColor, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            T(e.title, 15.sp, weight = FontWeight.Medium)
            Row {
                if (sub != null) T("$sub · ", 12.sp, c.secondary, modifier = Modifier.weight(1f, fill = false))
                T(whenText, 12.sp, if (ongoing) c.accent else barColor)
            }
        }
        Spacer(Modifier.width(8.dp))
        T("${hhmm(e.begin)}–${hhmm(e.end)}", 11.sp, c.secondary, mono = true)
    }
}

private fun eventsToday(n: Int): String {
    val w = when {
        n % 10 == 1 && n % 100 != 11 -> "событие"
        n % 10 in 2..4 && n % 100 !in 12..14 -> "события"
        else -> "событий"
    }
    return "$n $w сегодня"
}

@Composable
private fun WeatherCard(w: Weather?, city: City, now: Date, modifier: Modifier) {
    val c = LocalColors.current
    StandbyCard(modifier, padding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("Погода · ${city.name}", 14.sp, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (w != null) T(agoLabel(now.time - w.fetchedAt), 11.sp, c.secondary)
        }
        if (w == null) {
            Spacer(Modifier.weight(1f)); T("загрузка…", 14.sp, c.secondary); Spacer(Modifier.weight(1f))
            return@StandbyCard
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            T("${w.tempC}°", 38.sp, weight = FontWeight.Bold, mono = true)
            T("C", 16.sp, c.secondary, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                WeatherIcon(w.kind, w.isDay, c.accent, c.secondary, 28.dp)
                T(w.label, 13.sp)
                T(w.precipitation, 11.sp, c.secondary)
            }
        }
        Row(
            Modifier.fillMaxWidth().background(c.surfaceHigh, CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            T("Мин ${w.min}° · Макс ${w.max}°", 12.sp, c.secondary, modifier = Modifier.weight(1f))
            Icon(Ic.DROP, c.secondary, 11.dp)
            Spacer(Modifier.width(4.dp))
            T("${w.humidity}%", 12.sp, mono = true)
        }
    }
}

private fun agoLabel(ms: Long): String {
    val m = (ms / 60_000).toInt()
    return if (m < 1) "сейчас" else "${duration(ms)} назад"
}

@Composable
private fun RatesCard(r: Rates?, modifier: Modifier) {
    val c = LocalColors.current
    StandbyCard(modifier, padding = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Ic.RATES, c.positive, 16.dp)
            Spacer(Modifier.width(6.dp))
            T("Курсы валют (TJS)", 14.sp, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (r != null) T(SimpleDateFormat("d MMM", RU).format(Date(r.updatedAt)), 11.sp, c.secondary)
        }
        if (r == null) {
            Spacer(Modifier.weight(1f)); T("загрузка…", 14.sp, c.secondary); Spacer(Modifier.weight(1f))
            return@StandbyCard
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            r.items.forEach { rate ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(rate.code, 13.sp, weight = FontWeight.Bold, mono = true, modifier = Modifier.width(66.dp))
                    T(rate.name, 12.sp, c.secondary, modifier = Modifier.weight(1f))
                    T(String.format(RU, "%.2f", rate.value), 14.sp, weight = FontWeight.Bold, mono = true)
                    Spacer(Modifier.width(4.dp))
                    val d = rate.delta
                    when {
                        d == null || kotlin.math.abs(d) < 0.005 -> Icon(Ic.FLAT, c.secondary, 10.dp)
                        d > 0 -> Icon(Ic.UP, c.positive, 10.dp)
                        else -> Icon(Ic.DOWN, c.negative, 10.dp)
                    }
                }
            }
        }
        T("Рыночный ориентир, не курс НБТ", 10.sp, c.secondary)
    }
}

@Composable
private fun ColumnScope.EmptyState(title: String, hint: String) {
    val c = LocalColors.current
    Spacer(Modifier.weight(1f))
    T(title, 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    T(hint, 13.sp, c.secondary, modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.weight(1f))
}

// ═════════════════════════ Экран 3: медиаплеер ═════════════════════════

@Composable
private fun MediaPage(
    np: NowPlaying?, granted: Boolean, alarm: Long?, battery: BatteryInfo, now: Date,
    chrome: Boolean, page: Int, sleepAt: Long?, onSleep: (Long?) -> Unit
) {
    val c = LocalColors.current
    val glow = np?.artColor?.let { Color(it) }
    Box(
        Modifier.fillMaxSize().drawBehind {
            // Мягкая подсветка цветом обложки (≤15%), ночью выключена
            if (glow != null && !c.isNight) {
                drawRect(
                    Brush.radialGradient(
                        listOf(glow.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width * 0.22f, size.height * 0.55f),
                        radius = size.height * 0.9f
                    )
                )
            }
        }
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            TopBar(alarm, battery, now, chrome, page) {
                if (np != null) Pill { Icon(Ic.MUSIC, c.positive, 14.dp); T(np.app, 13.sp) }
            }
            if (np == null) {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Ic.MUSIC, c.secondary, 44.dp)
                    Spacer(Modifier.height(12.dp))
                    if (granted) {
                        T("Ничего не играет", 20.sp, weight = FontWeight.Medium)
                        T("Запусти Spotify или другой плеер", 14.sp, c.secondary)
                    } else {
                        T("Нет доступа к музыке", 20.sp, weight = FontWeight.Medium)
                        T("Standby → Разрешения → Музыка и плеер", 14.sp, c.secondary)
                    }
                }
                return@Column
            }
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArtWithVinyl(np, Modifier.fillMaxHeight())
                Spacer(Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Label("Сейчас играет", c.accent)
                    Spacer(Modifier.height(4.dp))
                    T(np.title, 26.sp, weight = FontWeight.Bold, maxLines = 2)
                    T(listOf(np.artist, np.album).filter { it.isNotBlank() }.joinToString(" · "), 16.sp, c.secondary)
                    Spacer(Modifier.height(14.dp))
                    var tick by remember { mutableIntStateOf(0) }
                    LaunchedEffect(np.playing) { while (np.playing) { delay(1000); tick++ } }
                    if (np.durationMs > 0) {
                        val pos = tick.let { np.livePosition() }
                        SeekBar(pos, np.durationMs) { np.seekTo(it) }
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            T(mmss(pos), 12.sp, weight = FontWeight.Bold, mono = true)
                            Spacer(Modifier.weight(1f))
                            T("-" + mmss(np.durationMs - pos), 12.sp, c.secondary, mono = true)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Controls(np, sleepAt, now, onSleep)
                }
            }
        }
    }
}

/** Обложка + выглядывающая справа пластинка (крутится, пока играет). */
@Composable
private fun ArtWithVinyl(np: NowPlaying, modifier: Modifier) {
    val c = LocalColors.current
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(np.playing) {
        while (np.playing && isActive) { delay(50); angle = (angle + 2.25f) % 360f } // 1 оборот за 8 с
    }
    // Ночью обложка уходит в монохромный красный и приглушается
    val nightFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.30f, 0.59f, 0.11f, 0f, 0f,
                    0.02f, 0.04f, 0.01f, 0f, 0f,
                    0.02f, 0.04f, 0.01f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    Box(modifier.aspectRatio(1.28f), contentAlignment = Alignment.CenterStart) {
        Canvas(Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.92f).aspectRatio(1f).rotate(angle)) {
            val r = size.minDimension / 2
            drawCircle(Color(0xFF0B0B0C), r)
            for (i in 1..9) drawCircle(Color.White.copy(alpha = 0.035f), r * (0.45f + i * 0.055f), style = Stroke(1f))
            drawCircle(np.artColor?.let { Color(it) }?.takeIf { !c.isNight } ?: c.secondary, r * 0.3f)
            drawCircle(Color.White.copy(alpha = 0.25f), r * 0.3f, center.copy(x = center.x + r * 0.12f), style = Stroke(1.5f))
            drawCircle(Color.Black, r * 0.04f)
        }
        Box(
            Modifier.fillMaxHeight().aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(c.surfaceHigh)
                .border(1.dp, c.outline, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            val art = np.art
            if (art != null) {
                Image(
                    art.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                    colorFilter = if (c.isNight) nightFilter else null,
                    modifier = Modifier.fillMaxSize().alpha(if (c.isNight) 0.6f else 1f)
                )
            } else {
                Icon(Ic.MUSIC, c.secondary, 64.dp)
            }
        }
    }
}

@Composable
private fun SeekBar(pos: Long, dur: Long, onSeek: (Long) -> Unit) {
    val c = LocalColors.current
    val frac = (pos.toFloat() / dur).coerceIn(0f, 1f)
    Canvas(
        Modifier.fillMaxWidth().height(18.dp).pointerInput(dur) {
            detectTapGestures { o -> onSeek((o.x / size.width * dur).toLong()) }
        }
    ) {
        val y = size.height / 2
        val h = 4.dp.toPx()
        drawLine(c.surfaceHigh, Offset(0f, y), Offset(size.width, y), h, androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(c.accent, Offset(0f, y), Offset(size.width * frac, y), h, androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(c.primary, 6.dp.toPx(), Offset(size.width * frac, y))
        drawCircle(c.accent, 3.dp.toPx(), Offset(size.width * frac, y))
    }
}

@Composable
private fun Controls(np: NowPlaying, sleepAt: Long?, now: Date, onSleep: (Long?) -> Unit) {
    val c = LocalColors.current
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        // Таймер сна: выкл → 15 → 30 → 60 → выкл
        Pill(
            Modifier.clip(CircleShape).clickable {
                val left = sleepAt?.let { ((it - System.currentTimeMillis()) / MIN).toInt() + 1 }
                val next = when {
                    left == null -> 15
                    left <= 15 -> 30
                    left <= 30 -> 60
                    else -> null
                }
                onSleep(next?.let { System.currentTimeMillis() + it * MIN })
            },
            bg = c.surfaceHigh
        ) {
            Icon(Ic.TIMER, if (sleepAt != null) c.accent else c.secondary, 16.dp)
            T(sleepAt?.let { "${((it - now.time) / MIN) + 1}м" } ?: "Сон", 13.sp, if (sleepAt != null) c.accent else c.secondary, mono = sleepAt != null)
        }
        RoundButton(Ic.PREV, 50.dp) { np.previous() }
        RoundButton(if (np.playing) Ic.PAUSE else Ic.PLAY, 70.dp, primary = true) { np.playPause() }
        RoundButton(Ic.NEXT, 50.dp) { np.next() }
        RoundButton(Ic.VOLUME, 46.dp) {
            ctx.getSystemService(AudioManager::class.java)
                ?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }
    }
}

@Composable
private fun RoundButton(ic: Ic, size: Dp, primary: Boolean = false, onClick: () -> Unit) {
    val c = LocalColors.current
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (primary) c.primary else c.surfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(ic, if (primary) c.onPrimaryButton else c.primary, size * 0.42f)
    }
}

// ═════════════════════════ Источники состояния ═════════════════════════

/** Периодически вызывает loader. keepLast — не затирать последнее удачное значение при ошибке. */
@Composable
private fun <T> rememberPolled(periodMs: Long, keepLast: Boolean = true, loader: suspend () -> T?): State<T?> {
    val state = remember { mutableStateOf<T?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            val v = loader()
            if (v != null || !keepLast) state.value = v
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

/** Сдвиг всего контента на ±8 dp раз в 2 минуты — против выгорания AMOLED. */
@Composable
private fun rememberBurnInShift(enabled: Boolean): State<Pair<Dp, Dp>> {
    val state = remember { mutableStateOf(0.dp to 0.dp) }
    LaunchedEffect(enabled) {
        while (enabled) {
            delay(2 * MIN)
            state.value = Random.nextInt(-8, 9).dp to Random.nextInt(-8, 9).dp
        }
    }
    return state
}

@Composable
private fun rememberBattery(): State<BatteryInfo> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(BatteryInfo(0, false, "")) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                i ?: return
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                val charging = plugged != 0
                val source = when {
                    status == BatteryManager.BATTERY_STATUS_FULL -> "Заряжено"
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Беспроводная"
                    plugged == BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    else -> "Зарядка"
                }
                state.value = BatteryInfo(level * 100 / scale, charging, source)
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { receiver.onReceive(context, it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
