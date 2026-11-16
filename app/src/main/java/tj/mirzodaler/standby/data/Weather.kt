package tj.mirzodaler.standby.data

import kotlin.math.roundToInt

enum class WeatherKind { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDER }

data class Weather(
    val tempC: Int,
    val code: Int,
    val min: Int,
    val max: Int,
    val humidity: Int,
    val isDay: Boolean,
    val fetchedAt: Long
) {
    val kind: WeatherKind
        get() = when (code) {
            0 -> WeatherKind.CLEAR
            1, 2 -> WeatherKind.PARTLY
            3 -> WeatherKind.CLOUDY
            45, 48 -> WeatherKind.FOG
            in 51..57 -> WeatherKind.DRIZZLE
            in 61..67, in 80..82 -> WeatherKind.RAIN
            in 71..77, 85, 86 -> WeatherKind.SNOW
            in 95..99 -> WeatherKind.THUNDER
            else -> WeatherKind.CLOUDY
        }

    val label: String
        get() = when (kind) {
            WeatherKind.CLEAR -> "Ясно"
            WeatherKind.PARTLY -> "Малооблачно"
            WeatherKind.CLOUDY -> "Пасмурно"
            WeatherKind.FOG -> "Туман"
            WeatherKind.DRIZZLE -> "Морось"
            WeatherKind.RAIN -> "Дождь"
            WeatherKind.SNOW -> "Снег"
            WeatherKind.THUNDER -> "Гроза"
        }

    val precipitation: String
        get() = when (kind) {
            WeatherKind.CLEAR, WeatherKind.PARTLY, WeatherKind.CLOUDY, WeatherKind.FOG -> "без осадков"
            else -> "осадки"
        }
}

/** Open-Meteo: бесплатно, без ключа и регистрации. */
object WeatherRepo {
    suspend fun load(city: City): Weather? {
        val json = getJson(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${city.lat}&longitude=${city.lon}" +
                "&current=temperature_2m,weather_code,relative_humidity_2m,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto"
        ) ?: return null
        return runCatching {
            val c = json.getJSONObject("current")
            val d = json.getJSONObject("daily")
            Weather(
                tempC = c.getDouble("temperature_2m").roundToInt(),
                code = c.getInt("weather_code"),
                min = d.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                max = d.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                humidity = c.optInt("relative_humidity_2m", 0),
                isDay = c.optInt("is_day", 1) == 1,
                fetchedAt = System.currentTimeMillis()
            )
        }.getOrNull()
    }
}
