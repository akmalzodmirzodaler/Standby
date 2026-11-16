package tj.mirzodaler.standby.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tj.mirzodaler.standby.data.WeatherKind

/** Векторные иконки, нарисованные кодом (сетка 24×24). Без библиотек и растров. */
enum class Ic { PLAY, PAUSE, NEXT, PREV, BOLT, ALARM, MOON, TIMER, VOLUME, PIN, CALENDAR, DROP, UP, DOWN, FLAT, RATES, MUSIC, SHIELD, CHECK }

@Composable
fun Icon(ic: Ic, color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        drawIcon(ic, color, s)
    }
}

private fun p(s: Float, vararg pts: Float): Path = Path().apply {
    moveTo(pts[0] * s, pts[1] * s)
    var i = 2
    while (i < pts.size) { lineTo(pts[i] * s, pts[i + 1] * s); i += 2 }
    close()
}

private fun DrawScope.drawIcon(ic: Ic, c: Color, s: Float) {
    val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round)
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    when (ic) {
        Ic.PLAY -> drawPath(p(s, 7f, 4f, 20f, 12f, 7f, 20f), c)
        Ic.PAUSE -> {
            drawRoundRect(c, o(6f, 4f), Size(4.5f * s, 16f * s), CornerRadius(1.2f * s))
            drawRoundRect(c, o(13.5f, 4f), Size(4.5f * s, 16f * s), CornerRadius(1.2f * s))
        }
        Ic.NEXT -> {
            drawPath(p(s, 5f, 5f, 16f, 12f, 5f, 19f), c)
            drawRoundRect(c, o(16.5f, 5f), Size(2.5f * s, 14f * s), CornerRadius(1f * s))
        }
        Ic.PREV -> {
            drawPath(p(s, 19f, 5f, 8f, 12f, 19f, 19f), c)
            drawRoundRect(c, o(5f, 5f), Size(2.5f * s, 14f * s), CornerRadius(1f * s))
        }
        Ic.BOLT -> drawPath(p(s, 13.5f, 2f, 4.5f, 13.5f, 11f, 13.5f, 10f, 22f, 19.5f, 9.5f, 13f, 9.5f), c)
        Ic.ALARM -> {
            drawCircle(c, 7.5f * s, o(12f, 13f), style = stroke)
            drawLine(c, o(12f, 13f), o(12f, 9f), 2f * s, StrokeCap.Round)
            drawLine(c, o(12f, 13f), o(14.8f, 14.8f), 2f * s, StrokeCap.Round)
            drawLine(c, o(3.5f, 5.5f), o(6.5f, 3f), 2f * s, StrokeCap.Round)
            drawLine(c, o(20.5f, 5.5f), o(17.5f, 3f), 2f * s, StrokeCap.Round)
        }
        Ic.MOON -> {
            val a = Path().apply { addOval(Rect(o(3f, 3f), Size(18f * s, 18f * s))) }
            val b = Path().apply { addOval(Rect(o(9f, 0.5f), Size(16f * s, 16f * s))) }
            drawPath(Path.combine(PathOperation.Difference, a, b), c)
        }
        Ic.TIMER -> {
            drawCircle(c, 8f * s, o(12f, 13.5f), style = stroke)
            drawLine(c, o(12f, 13.5f), o(12f, 9f), 2f * s, StrokeCap.Round)
            drawLine(c, o(9.5f, 2.5f), o(14.5f, 2.5f), 2f * s, StrokeCap.Round)
        }
        Ic.VOLUME -> {
            drawPath(p(s, 3f, 9f, 7f, 9f, 12f, 4.5f, 12f, 19.5f, 7f, 15f, 3f, 15f), c)
            drawArc(c, -50f, 100f, false, o(9f, 7f), Size(8f * s, 10f * s), style = stroke)
            drawArc(c, -55f, 110f, false, o(9f, 3.5f), Size(13f * s, 17f * s), style = stroke)
        }
        Ic.PIN -> {
            drawPath(p(s, 6.2f, 12.5f, 12f, 22f, 17.8f, 12.5f), c)
            drawCircle(c, 6.5f * s, o(12f, 9f))
            drawCircle(Color.Black, 2.4f * s, o(12f, 9f))
        }
        Ic.CALENDAR -> {
            drawRoundRect(c, o(3.5f, 5f), Size(17f * s, 16f * s), CornerRadius(3f * s), style = stroke)
            drawLine(c, o(3.5f, 10f), o(20.5f, 10f), 2f * s)
            drawLine(c, o(8f, 3f), o(8f, 6.5f), 2f * s, StrokeCap.Round)
            drawLine(c, o(16f, 3f), o(16f, 6.5f), 2f * s, StrokeCap.Round)
        }
        Ic.DROP -> {
            drawPath(p(s, 12f, 2.5f, 6.3f, 12.5f, 17.7f, 12.5f), c)
            drawCircle(c, 6f * s, o(12f, 15f))
        }
        Ic.UP -> drawPath(p(s, 12f, 6f, 19f, 17f, 5f, 17f), c)
        Ic.DOWN -> drawPath(p(s, 5f, 7f, 19f, 7f, 12f, 18f), c)
        Ic.FLAT -> drawLine(c, o(6f, 12f), o(18f, 12f), 2.4f * s, StrokeCap.Round)
        Ic.RATES -> {
            drawArc(c, 200f, 230f, false, o(4f, 4f), Size(16f * s, 16f * s), style = stroke)
            drawPath(p(s, 2f, 10f, 6f, 6f, 8f, 11f), c)
            drawLine(c, o(12f, 8f), o(12f, 16f), 2f * s, StrokeCap.Round)
        }
        Ic.MUSIC -> {
            drawLine(c, o(10f, 17f), o(10f, 4f), 2f * s, StrokeCap.Round)
            drawLine(c, o(10f, 4f), o(19f, 2.5f), 2f * s, StrokeCap.Round)
            drawLine(c, o(19f, 2.5f), o(19f, 15f), 2f * s, StrokeCap.Round)
            drawCircle(c, 3f * s, o(7f, 17.5f))
            drawCircle(c, 3f * s, o(16f, 15.5f))
        }
        Ic.SHIELD -> {
            drawPath(p(s, 12f, 2.5f, 20f, 5.5f, 19f, 13f, 12f, 21.5f, 5f, 13f, 4f, 5.5f), c, style = stroke)
            drawLine(c, o(8.5f, 12f), o(11f, 14.5f), 2f * s, StrokeCap.Round)
            drawLine(c, o(11f, 14.5f), o(15.5f, 9.5f), 2f * s, StrokeCap.Round)
        }
        Ic.CHECK -> {
            drawCircle(c, 9f * s, o(12f, 12f), style = stroke)
            drawLine(c, o(8f, 12.5f), o(11f, 15.3f), 2f * s, StrokeCap.Round)
            drawLine(c, o(11f, 15.3f), o(16.2f, 9.2f), 2f * s, StrokeCap.Round)
        }
    }
}

/** Плоские погодные иконки: без градиентов, читаются с 2 метров. */
@Composable
fun WeatherIcon(kind: WeatherKind, isDay: Boolean, sun: Color, cloud: Color, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        fun o(x: Float, y: Float) = Offset(x * s, y * s)
        fun sunAt(cx: Float, cy: Float, r: Float) {
            if (isDay) {
                drawCircle(sun, r * s, o(cx, cy))
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val x1 = cx + (r + 1.8f) * Math.cos(a).toFloat()
                    val y1 = cy + (r + 1.8f) * Math.sin(a).toFloat()
                    val x2 = cx + (r + 4f) * Math.cos(a).toFloat()
                    val y2 = cy + (r + 4f) * Math.sin(a).toFloat()
                    drawLine(sun, o(x1, y1), o(x2, y2), 1.8f * s, StrokeCap.Round)
                }
            } else {
                val a = Path().apply { addOval(Rect(o(cx - r - 1, cy - r - 1), Size((2 * r + 2) * s, (2 * r + 2) * s))) }
                val b = Path().apply { addOval(Rect(o(cx - r + 3, cy - r - 3), Size((2 * r + 1) * s, (2 * r + 1) * s))) }
                drawPath(Path.combine(PathOperation.Difference, a, b), sun)
            }
        }
        fun cloudAt(dy: Float = 0f) {
            drawCircle(cloud, 4f * s, o(8.5f, 13f + dy))
            drawCircle(cloud, 5.5f * s, o(13.5f, 11f + dy))
            drawCircle(cloud, 3.8f * s, o(18f, 14f + dy))
            drawRoundRect(cloud, o(4.5f, 13f + dy), Size(17.3f * s, 4.8f * s), CornerRadius(2.4f * s))
        }
        when (kind) {
            WeatherKind.CLEAR -> sunAt(12f, 12f, 5f)
            WeatherKind.PARTLY -> { sunAt(8f, 8f, 3.6f); cloudAt(2f) }
            WeatherKind.CLOUDY -> cloudAt()
            WeatherKind.FOG -> {
                for (i in 0..3) {
                    val y = 7f + i * 3.6f
                    drawLine(cloud, o(4f + (i % 2) * 2f, y), o(20f - (i % 2) * 2f, y), 2f * s, StrokeCap.Round)
                }
            }
            WeatherKind.DRIZZLE, WeatherKind.RAIN -> {
                cloudAt(-3f)
                val n = if (kind == WeatherKind.RAIN) 3 else 2
                for (i in 0 until n) {
                    val x = 8.5f + i * 4f
                    drawLine(sun, o(x, 17.5f), o(x - 1.5f, 21.5f), 1.8f * s, StrokeCap.Round)
                }
            }
            WeatherKind.SNOW -> {
                cloudAt(-3f)
                for (i in 0 until 3) drawCircle(sun, 1.3f * s, o(8f + i * 4f, 19.5f + (i % 2) * 1.5f))
            }
            WeatherKind.THUNDER -> {
                cloudAt(-3f)
                drawPath(p(s, 12.5f, 15f, 9f, 20f, 12f, 20f, 11f, 23.5f, 15.5f, 18f, 12.5f, 18f), sun)
            }
        }
        Unit
    }
}
