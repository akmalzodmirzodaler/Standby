package tj.mirzodaler.standby.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import tj.mirzodaler.standby.data.ClockStyle
import java.util.Calendar
import java.util.Date
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private data class Hms(val h: Int, val m: Int, val s: Int)

private fun Date.hms(): Hms {
    val c = Calendar.getInstance().apply { time = this@hms }
    return Hms(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND))
}

/** Циферблат выбранного стиля. Заполняет отведённую ему область. */
@Composable
fun ClockFace(style: ClockStyle, now: Date, showSeconds: Boolean, modifier: Modifier = Modifier) {
    val t = now.hms()
    when (style) {
        ClockStyle.DIGITAL -> DigitalBold(t, showSeconds, modifier)
        ClockStyle.CLASSIC -> ClassicSerif(t, modifier)
        ClockStyle.FLIP -> FlipClock(t, modifier)
        ClockStyle.STACKED -> Stacked(t, modifier)
        ClockStyle.ANALOG -> AnalogClassic(t, showSeconds, modifier)
        ClockStyle.ANALOG_MINIMAL -> AnalogMinimal(t, showSeconds, modifier)
    }
}

val ClockStyle.isAnalog get() = this == ClockStyle.ANALOG || this == ClockStyle.ANALOG_MINIMAL

@Composable
private fun Dp.asSp(): TextUnit = with(LocalDensity.current) { this@asSp.toSp() }

// ───────────── 1. Digital Bold (Space Mono) ─────────────

@Composable
private fun DigitalBold(t: Hms, showSeconds: Boolean, modifier: Modifier) {
    val c = LocalColors.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Space Mono: ширина знака ≈ 0.6em → «HH:mm» ≈ 3em
        val size = minOf(maxHeight * 0.8f, maxWidth / (if (showSeconds) 3.6f else 3.15f))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "%02d:%02d".format(t.h, t.m),
                color = c.primary, fontFamily = Fonts.Mono, fontWeight = FontWeight.Bold,
                fontSize = size.asSp(), lineHeight = size.asSp(), letterSpacing = (-0.04).em,
                maxLines = 1, softWrap = false
            )
            if (showSeconds) {
                Column(Modifier.padding(top = size * 0.08f, start = 6.dp)) {
                    Box(
                        Modifier.background(c.surfaceHigh, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            ":%02d".format(t.s), color = c.accent, fontFamily = Fonts.Mono,
                            fontWeight = FontWeight.Bold, fontSize = (size * 0.13f).asSp()
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.width(size * 0.3f).height(3.dp).background(c.surfaceHigh, CircleShape)) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth((t.s + 1) / 60f)
                                .background(c.positive, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// ───────────── 2. Классика (DM Serif Display) ─────────────

@Composable
private fun ClassicSerif(t: Hms, modifier: Modifier) {
    val c = LocalColors.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val size = minOf(maxHeight * 0.95f, maxWidth / 2.75f)
        // Цифры в ячейках фиксированной ширины — пропорциональный шрифт не «прыгает»
        Row(verticalAlignment = Alignment.CenterVertically) {
            val text = "%02d%02d".format(t.h, t.m)
            FixedDigit(text[0], size, 0.6f, Fonts.Serif, c.primary)
            FixedDigit(text[1], size, 0.6f, Fonts.Serif, c.primary)
            // Двоеточие мягко «дышит» раз в секунду
            Column(
                Modifier.padding(horizontal = size * 0.06f).alpha(if (t.s % 2 == 0) 1f else 0.35f),
                verticalArrangement = Arrangement.spacedBy(size * 0.2f)
            ) {
                Box(Modifier.size(size * 0.11f).background(c.primary, CircleShape))
                Box(Modifier.size(size * 0.11f).background(c.primary, CircleShape))
            }
            FixedDigit(text[2], size, 0.6f, Fonts.Serif, c.primary)
            FixedDigit(text[3], size, 0.6f, Fonts.Serif, c.primary)
        }
    }
}

@Composable
private fun FixedDigit(ch: Char, size: Dp, widthEm: Float, family: FontFamily, color: Color, weight: FontWeight = FontWeight.Normal) {
    Box(Modifier.width(size * widthEm), contentAlignment = Alignment.Center) {
        Text(
            ch.toString(), color = color, fontFamily = family, fontWeight = weight,
            fontSize = size.asSp(), lineHeight = size.asSp(), maxLines = 1, softWrap = false
        )
    }
}

// ───────────── 3. Перекидные (Flip / Split-flap) ─────────────

@Composable
private fun FlipClock(t: Hms, modifier: Modifier) {
    val c = LocalColors.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val gap = 10.dp
        val tileH = minOf(maxHeight * 0.92f, (maxWidth - gap * 3 - 40.dp) / 4f / 0.78f)
        val tileW = tileH * 0.78f
        val text = "%02d%02d".format(t.h, t.m)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
            FlipTile(text[0], tileW, tileH)
            FlipTile(text[1], tileW, tileH)
            Column(
                Modifier.padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(tileH * 0.16f)
            ) {
                Box(Modifier.size(tileH * 0.08f).background(c.accent, CircleShape))
                Box(Modifier.size(tileH * 0.08f).background(c.accent, CircleShape))
            }
            FlipTile(text[2], tileW, tileH)
            FlipTile(text[3], tileW, tileH)
        }
    }
}

@Composable
private fun FlipTile(digit: Char, w: Dp, h: Dp) {
    val c = LocalColors.current
    val flip = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(digit) {
        if (first) { first = false; return@LaunchedEffect }
        flip.snapTo(-90f)
        flip.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
    }
    val density = LocalDensity.current.density
    Box(
        Modifier.size(w, h)
            .background(
                Brush.verticalGradient(listOf(c.surfaceHigh, c.surface)),
                RoundedCornerShape(h * 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            digit.toString(), color = c.primary, fontFamily = Fonts.Condensed,
            fontWeight = FontWeight.SemiBold, fontSize = (h * 0.72f).asSp(), lineHeight = (h * 0.8f).asSp(),
            modifier = Modifier.graphicsLayer {
                rotationX = flip.value
                cameraDistance = 14f * density
            }
        )
        // Шов между створками
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
    }
}

// ───────────── 4. Вертикальные (часы над минутами) ─────────────

@Composable
private fun Stacked(t: Hms, modifier: Modifier) {
    val c = LocalColors.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val size = minOf(maxHeight * 0.5f, maxWidth / 1.4f)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "%02d".format(t.h), color = c.primary, fontFamily = Fonts.Mono, fontWeight = FontWeight.Bold,
                fontSize = size.asSp(), lineHeight = (size * 0.92f).asSp(), letterSpacing = (-0.04).em
            )
            Text(
                "%02d".format(t.m), color = c.accent, fontFamily = Fonts.Mono, fontWeight = FontWeight.Bold,
                fontSize = size.asSp(), lineHeight = size.asSp(), letterSpacing = (-0.04).em
            )
        }
    }
}

// ───────────── 5. Аналоговые классические ─────────────

@Composable
private fun AnalogClassic(t: Hms, showSeconds: Boolean, modifier: Modifier) {
    val c = LocalColors.current
    val measurer = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val r = size.minDimension / 2f * 0.97f
        val ctr = center
        drawCircle(c.surface, r, ctr)
        drawCircle(c.outline, r, ctr, style = Stroke(1.5.dp.toPx()))

        for (i in 0 until 60) {
            val major = i % 5 == 0
            val len = if (major) r * 0.09f else r * 0.035f
            val w = if (major) r * 0.022f else r * 0.008f
            val a = Math.toRadians(i * 6.0 - 90)
            val outer = r * 0.93f
            drawLine(
                if (major) c.primary else c.secondary,
                Offset(ctr.x + (outer - len) * cos(a).toFloat(), ctr.y + (outer - len) * sin(a).toFloat()),
                Offset(ctr.x + outer * cos(a).toFloat(), ctr.y + outer * sin(a).toFloat()),
                w, StrokeCap.Round
            )
        }
        val numStyle = TextStyle(color = c.primary, fontSize = (r * 0.15f).toSp(), fontWeight = FontWeight.Medium)
        for (n in 1..12) {
            val a = Math.toRadians(n * 30.0 - 90)
            val layout = measurer.measure(n.toString(), numStyle)
            val pr = r * 0.7f
            drawText(
                layout,
                topLeft = Offset(
                    ctr.x + pr * cos(a).toFloat() - layout.size.width / 2f,
                    ctr.y + pr * sin(a).toFloat() - layout.size.height / 2f
                )
            )
        }
        hands(t, r, c, showSeconds, hourW = r * 0.06f, minW = r * 0.035f)
    }
}

// ───────────── 6. Аналоговые минимал (Braun-style) ─────────────

@Composable
private fun AnalogMinimal(t: Hms, showSeconds: Boolean, modifier: Modifier) {
    val c = LocalColors.current
    Canvas(modifier.aspectRatio(1f)) {
        val r = size.minDimension / 2f * 0.97f
        val ctr = center
        for (i in 0 until 12) {
            val quarter = i % 3 == 0
            val a = Math.toRadians(i * 30.0 - 90)
            val len = if (quarter) r * 0.16f else r * 0.08f
            drawLine(
                if (quarter) c.primary else c.secondary,
                Offset(ctr.x + (r - len) * cos(a).toFloat(), ctr.y + (r - len) * sin(a).toFloat()),
                Offset(ctr.x + r * cos(a).toFloat(), ctr.y + r * sin(a).toFloat()),
                if (quarter) r * 0.03f else r * 0.018f, StrokeCap.Round
            )
        }
        val hourA = (t.h % 12 + t.m / 60f) * 30f
        val minA = (t.m + t.s / 60f) * 6f
        rotate(hourA, ctr) {
            drawLine(c.secondary, ctr, Offset(ctr.x, ctr.y - r * 0.5f), r * 0.07f, StrokeCap.Round)
        }
        rotate(minA, ctr) {
            drawLine(c.primary, ctr, Offset(ctr.x, ctr.y - r * 0.8f), r * 0.03f, StrokeCap.Round)
        }
        // Секунды — точка, бегущая по кругу
        if (showSeconds) {
            val a = Math.toRadians(t.s * 6.0 - 90)
            drawCircle(c.accent, r * 0.035f, Offset(ctr.x + r * 0.88f * cos(a).toFloat(), ctr.y + r * 0.88f * sin(a).toFloat()))
        }
        drawCircle(c.primary, r * 0.045f, ctr)
        drawCircle(Color.Black, r * 0.02f, ctr)
    }
}

private fun DrawScope.hands(t: Hms, r: Float, c: StandbyColors, showSeconds: Boolean, hourW: Float, minW: Float) {
    val ctr = center
    val hourA = (t.h % 12 + t.m / 60f) * 30f
    val minA = (t.m + t.s / 60f) * 6f
    rotate(hourA, ctr) {
        drawLine(c.primary, Offset(ctr.x, ctr.y + r * 0.08f), Offset(ctr.x, ctr.y - r * 0.5f), hourW, StrokeCap.Round)
    }
    rotate(minA, ctr) {
        drawLine(c.primary, Offset(ctr.x, ctr.y + r * 0.1f), Offset(ctr.x, ctr.y - r * 0.8f), minW, StrokeCap.Round)
    }
    if (showSeconds) {
        rotate(t.s * 6f, ctr) {
            drawLine(c.accent, Offset(ctr.x, ctr.y + r * 0.18f), Offset(ctr.x, ctr.y - r * 0.86f), r * 0.012f, StrokeCap.Round)
        }
    }
    drawCircle(c.accent, r * 0.04f, ctr)
    drawCircle(Color.Black, r * 0.016f, ctr)
}

/** Мини-превью стиля для экрана настроек. */
@Composable
fun ClockPreview(style: ClockStyle, now: Date, modifier: Modifier = Modifier) {
    ClockFace(style, now, showSeconds = style.isAnalog, modifier = modifier)
}

@Suppress("unused")
private fun clampF(v: Float, a: Float, b: Float) = min(maxOf(v, a), b)
