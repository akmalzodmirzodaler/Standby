package tj.mirzodaler.standby.data

/**
 * Курсы к сомони (TJS). Источник: open.er-api.com — бесплатно, без ключа, обновляется раз в сутки.
 * Это рыночный ориентир, НЕ официальный учётный курс НБТ.
 */
data class Rates(
    val usd: Double,     // 1 USD в TJS
    val eur: Double,     // 1 EUR в TJS
    val rub100: Double,  // 100 RUB в TJS
    val updated: String  // дата обновления у источника
)

object RatesRepo {
    suspend fun load(): Rates? {
        val json = getJson("https://open.er-api.com/v6/latest/USD") ?: return null
        return runCatching {
            val r = json.getJSONObject("rates")
            val tjs = r.getDouble("TJS")
            Rates(
                usd = tjs,
                eur = tjs / r.getDouble("EUR"),
                rub100 = tjs / r.getDouble("RUB") * 100,
                updated = json.optString("time_last_update_utc")
            )
        }.getOrNull()
    }
}
