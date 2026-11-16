package tj.mirzodaler.standby.data

import android.content.Context

enum class ClockStyle(val title: String) {
    DIGITAL("Digital Bold"),
    CLASSIC("Классика"),
    FLIP("Перекидные"),
    STACKED("Вертикальные"),
    ANALOG("Аналоговые"),
    ANALOG_MINIMAL("Аналог-минимал");

    fun next(): ClockStyle = entries[(ordinal + 1) % entries.size]
}

data class City(val name: String, val lat: Double, val lon: Double, val utc: String)

val CITIES = listOf(
    City("Худжанд", 40.28, 69.63, "UTC+5"),
    City("Душанбе", 38.56, 68.77, "UTC+5"),
    City("Истаравшан", 39.91, 69.00, "UTC+5"),
    City("Ташкент", 41.31, 69.28, "UTC+5"),
    City("Москва", 55.76, 37.62, "UTC+3")
)

/** Пресеты ночного режима: (с часа, до часа); null — выключен. */
val NIGHT_PRESETS: List<Pair<Int, Int>?> = listOf(22 to 7, 23 to 7, 21 to 6, 22 to 6, 0 to 7, null)

/** Настройки приложения. Заставка читает их при каждом запуске. */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("standby", Context.MODE_PRIVATE)

    var clockStyle: ClockStyle
        get() = runCatching { ClockStyle.valueOf(sp.getString("clock", null)!!) }.getOrDefault(ClockStyle.DIGITAL)
        set(v) = sp.edit().putString("clock", v.name).apply()

    var cityIndex: Int
        get() = sp.getInt("city", 0).coerceIn(0, CITIES.lastIndex)
        set(v) = sp.edit().putInt("city", v).apply()

    val city: City get() = CITIES[cityIndex]

    var nightPreset: Int
        get() = sp.getInt("night", 0).coerceIn(0, NIGHT_PRESETS.lastIndex)
        set(v) = sp.edit().putInt("night", v).apply()

    var showSeconds: Boolean
        get() = sp.getBoolean("seconds", true)
        set(v) = sp.edit().putBoolean("seconds", v).apply()

    var burnInShift: Boolean
        get() = sp.getBoolean("burnin", true)
        set(v) = sp.edit().putBoolean("burnin", v).apply()

    var previewed: Boolean
        get() = sp.getBoolean("previewed", false)
        set(v) = sp.edit().putBoolean("previewed", v).apply()

    fun isNight(hour: Int): Boolean {
        val (from, to) = NIGHT_PRESETS[nightPreset] ?: return false
        return if (from > to) hour >= from || hour < to else hour in from until to
    }

    // ── Снимки курсов для расчёта динамики за день ──
    internal fun getDouble(key: String): Double? =
        if (sp.contains(key)) Double.fromBits(sp.getLong(key, 0)) else null

    internal fun putDouble(key: String, v: Double) = sp.edit().putLong(key, v.toRawBits()).apply()
    internal fun getLong(key: String): Long = sp.getLong(key, 0)
    internal fun putLong(key: String, v: Long) = sp.edit().putLong(key, v).apply()
}

fun nightLabel(i: Int): String = NIGHT_PRESETS[i]?.let { (f, t) -> "%02d–%02d".format(f, t) } ?: "Выкл"
