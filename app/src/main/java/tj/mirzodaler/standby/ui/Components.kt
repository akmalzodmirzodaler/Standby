package tj.mirzodaler.standby.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val RU: Locale = Locale("ru")

/** Карточка: тональная подложка + 1px рамка, без теней (DESIGN.md → Elevation). */
@Composable
fun StandbyCard(modifier: Modifier = Modifier, padding: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalColors.current
    Column(
        modifier
            .background(c.surface, RoundedCornerShape(28.dp))
            .border(1.dp, c.outline, RoundedCornerShape(28.dp))
            .padding(padding.dp),
        content = content
    )
}

/** Плашка-«пилюля» в верхней панели. */
@Composable
fun Pill(modifier: Modifier = Modifier, bg: Color? = null, content: @Composable RowScope.() -> Unit) {
    val c = LocalColors.current
    Row(
        modifier
            .background(bg ?: c.surface, CircleShape)
            .border(1.dp, c.outline, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun Label(text: String, color: Color? = null, size: TextUnit = 12.sp, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(RU), color = color ?: LocalColors.current.secondary, fontSize = size,
        fontWeight = FontWeight.Medium, letterSpacing = 0.08.em, maxLines = 1, modifier = modifier
    )
}

@Composable
fun T(
    text: String, size: TextUnit, color: Color? = null, weight: FontWeight = FontWeight.Normal,
    mono: Boolean = false, maxLines: Int = 1, modifier: Modifier = Modifier
) {
    Text(
        text, color = color ?: LocalColors.current.primary, fontSize = size, fontWeight = weight,
        fontFamily = if (mono) Fonts.Mono else Fonts.Ui, maxLines = maxLines,
        overflow = TextOverflow.Ellipsis, modifier = modifier
    )
}

/** Будильник: «Будильник 07:30 · через 6ч 13м». */
@Composable
fun AlarmPill(alarm: Long?, now: Date, showCountdown: Boolean = false) {
    val c = LocalColors.current
    if (alarm == null) return
    Pill {
        Icon(if (c.isNight) Ic.MOON else Ic.ALARM, c.accent, 16.dp)
        T("Будильник " + SimpleDateFormat("HH:mm", RU).format(Date(alarm)), 14.sp, weight = FontWeight.Medium)
        if (showCountdown) T("через " + duration(alarm - now.time), 13.sp, c.secondary)
    }
}

/** Батарея: «⚡ 88% Зарядка». */
@Composable
fun BatteryPill(b: BatteryInfo) {
    val c = LocalColors.current
    Pill {
        if (b.charging) Icon(Ic.BOLT, c.positive, 16.dp)
        T("${b.level}%", 14.sp, if (b.charging) c.positive else c.primary, FontWeight.Bold, mono = true)
        if (b.charging) T(b.source, 13.sp, c.secondary)
    }
}

data class BatteryInfo(val level: Int, val charging: Boolean, val source: String)

/** Точки страниц. Видны недолго после касания — статичный элемент выжигал бы AMOLED. */
@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (i == current) 18.dp else 6.dp)
                    .background(if (i == current) c.primary else c.secondary.copy(alpha = 0.5f), CircleShape)
            )
        }
    }
}

// ───────── Форматирование ─────────

fun dateLong(d: Date): String =
    SimpleDateFormat("EEEE, d MMMM", RU).format(d).replaceFirstChar { it.titlecase(RU) }

fun weekday(d: Date): String =
    SimpleDateFormat("EEEE", RU).format(d).replaceFirstChar { it.titlecase(RU) }

fun dayMonthYear(d: Date): String = SimpleDateFormat("d MMMM yyyy", RU).format(d)

fun hhmm(ms: Long): String = SimpleDateFormat("HH:mm", RU).format(Date(ms))

/** «6ч 13м», «25м», «2д 4ч». */
fun duration(ms: Long): String {
    val m = (ms.coerceAtLeast(0) / 60_000).toInt()
    val d = m / 1440; val h = (m % 1440) / 60; val mm = m % 60
    return when {
        d > 0 -> "${d}д ${h}ч"
        h > 0 -> "${h}ч ${mm}м"
        else -> "${mm}м"
    }
}

fun isToday(ms: Long): Boolean {
    val a = Calendar.getInstance()
    val b = Calendar.getInstance().apply { timeInMillis = ms }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

fun mmss(ms: Long): String {
    val s = ms.coerceAtLeast(0) / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
