package tj.mirzodaler.standby.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import tj.mirzodaler.standby.R

/** Токены «Nocturne Standby» (DESIGN.md из макета). Раскладка одна, меняются только значения. */
data class StandbyColors(
    val bg: Color,
    val surface: Color,       // карточки
    val surfaceHigh: Color,   // кнопки, плашки внутри карточек
    val primary: Color,       // главные цифры и текст
    val secondary: Color,     // подписи, метаданные
    val accent: Color,        // будильник, прогресс, акценты
    val outline: Color,       // 1px рамка карточек
    val positive: Color,      // зарядка, рост курса
    val negative: Color,      // падение курса
    val onPrimaryButton: Color,
    val isNight: Boolean
)

val DayColors = StandbyColors(
    bg = Color.Black,
    surface = Color(0xFF161618),
    surfaceHigh = Color(0xFF222226),
    primary = Color.White,
    secondary = Color(0xFF9A9AA0),
    accent = Color(0xFFFF9F0A),
    outline = Color.White.copy(alpha = 0.12f),
    positive = Color(0xFF30D158),
    negative = Color(0xFFFF453A),
    onPrimaryButton = Color.Black,
    isNight = false
)

/** Ночь: монохромный красный спектр, никакого синего. */
val NightColors = StandbyColors(
    bg = Color.Black,
    surface = Color(0xFF120404),
    surfaceHigh = Color(0xFF1E0707),
    primary = Color(0xFFE53935),
    secondary = Color(0xFF7F1D1D),
    accent = Color(0xFFEF5350),
    outline = Color(0xFFD32F2F).copy(alpha = 0.20f),
    positive = Color(0xFFE53935),
    negative = Color(0xFF7F1D1D),
    onPrimaryButton = Color.Black,
    isNight = true
)

val LocalColors = staticCompositionLocalOf { DayColors }

object Fonts {
    /** Табличные цифры — часы не «дёргаются» при смене минуты. */
    val Mono = FontFamily(
        Font(R.font.space_mono_regular, FontWeight.Normal),
        Font(R.font.space_mono_bold, FontWeight.Bold)
    )
    val Serif = FontFamily(Font(R.font.dm_serif_display, FontWeight.Normal))
    val Condensed = FontFamily(Font(R.font.oswald_semibold, FontWeight.SemiBold))
    /** Текст интерфейса — системный шрифт One UI. */
    val Ui = FontFamily.Default
}
