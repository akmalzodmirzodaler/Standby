package tj.mirzodaler.standby.data

import kotlin.math.roundToInt

data class Weather(val tempC: Int, val code: Int) {
    val label: String get() = describe(code)
}

/** Open-Meteo: бесплатно, без ключа и регистрации. Координаты по умолчанию — Худжанд. */
object WeatherRepo {
    private const val LAT = 40.28
    private const val LON = 69.63

    suspend fun load(): Weather? {
        val json = getJson(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$LAT&longitude=$LON" +
                "&current=temperature_2m,weather_code&timezone=auto"
        ) ?: return null
        return runCatching {
            val current = json.getJSONObject("current")
            Weather(
                tempC = current.getDouble("temperature_2m").roundToInt(),
                code = current.getInt("weather_code")
            )
        }.getOrNull()
    }
}

/** Коды WMO → короткое описание. */
private fun describe(code: Int): String = when (code) {
    0 -> "Ясно"
    1, 2 -> "Малооблачно"
    3 -> "Пасмурно"
    45, 48 -> "Туман"
    in 51..57 -> "Морось"
    in 61..67, in 80..82 -> "Дождь"
    in 71..77, 85, 86 -> "Снег"
    in 95..99 -> "Гроза"
    else -> "—"
}
