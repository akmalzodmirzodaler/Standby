package tj.mirzodaler.standby.data

import android.content.Context

/**
 * Курсы к сомони (TJS). Источник: open.er-api.com — бесплатно, без ключа, обновляется раз в сутки.
 * Это рыночный ориентир, НЕ официальный учётный курс НБТ.
 *
 * Динамику источник не даёт, поэтому храним снимок прошлых суток у себя
 * и сравниваем новый курс с ним. Стрелки появятся со второго дня работы.
 */
data class Rate(val code: String, val name: String, val value: Double, val delta: Double?)

data class Rates(val items: List<Rate>, val updatedAt: Long)

object RatesRepo {
    private val CODES = listOf(
        Triple("USD", "Доллар США", 1.0),
        Triple("EUR", "Евро", 1.0),
        Triple("RUB", "Рубль", 100.0)
    )

    suspend fun load(ctx: Context): Rates? {
        val json = getJson("https://open.er-api.com/v6/latest/USD") ?: return null
        return runCatching {
            val r = json.getJSONObject("rates")
            val tjs = r.getDouble("TJS")
            val sourceTime = json.optLong("time_last_update_unix", 0L) * 1000
            val prefs = Prefs(ctx)
            val snapTime = prefs.getLong("rates_snap_time")

            val values = CODES.associate { (code, _, mult) ->
                code to tjs / r.getDouble(code) * mult
            }
            // Источник обновился → текущий снимок становится «вчерашним»
            if (sourceTime != 0L && sourceTime != snapTime) {
                if (snapTime != 0L) CODES.forEach { (code) ->
                    prefs.getDouble("snap_$code")?.let { prefs.putDouble("prev_$code", it) }
                }
                values.forEach { (code, v) -> prefs.putDouble("snap_$code", v) }
                prefs.putLong("rates_snap_time", sourceTime)
            }
            Rates(
                items = CODES.map { (code, name, mult) ->
                    val v = values.getValue(code)
                    Rate(
                        code = if (mult > 1) "${mult.toInt()} $code" else code,
                        name = name,
                        value = v,
                        delta = prefs.getDouble("prev_$code")?.let { v - it }
                    )
                },
                updatedAt = if (sourceTime != 0L) sourceTime else System.currentTimeMillis()
            )
        }.getOrNull()
    }
}
