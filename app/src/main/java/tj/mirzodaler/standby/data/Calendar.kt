package tj.mirzodaler.standby.data

import android.Manifest
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class CalEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
    val calendar: String,
    val location: String
)

data class Agenda(
    val timed: List<CalEvent>,   // ближайшие события со временем (до 3)
    val allDay: CalEvent?,       // одно событие «весь день» на сегодня
    val todayCount: Int          // сколько событий сегодня всего
)

/** Ближайшие события из всех календарей телефона (Samsung, Google, Outlook — что синхронизировано). */
object CalendarRepo {
    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** null — нет разрешения. */
    suspend fun agenda(ctx: Context): Agenda? = withContext(Dispatchers.IO) {
        if (!hasPermission(ctx)) return@withContext null
        val now = System.currentTimeMillis()
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis
        val until = now + 48L * 60 * 60 * 1000
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - 24L * 60 * 60 * 1000) // захватываем идущие сейчас
            ContentUris.appendId(it, until)
        }.build()
        val projection = arrayOf(
            Instances.TITLE, Instances.BEGIN, Instances.END, Instances.ALL_DAY,
            Instances.DISPLAY_COLOR, Instances.CALENDAR_DISPLAY_NAME, Instances.EVENT_LOCATION
        )
        val all = runCatching {
            ctx.contentResolver.query(uri, projection, null, null, "${Instances.BEGIN} ASC")?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(
                            CalEvent(
                                title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "Без названия",
                                begin = c.getLong(1),
                                end = c.getLong(2),
                                allDay = c.getInt(3) == 1,
                                color = c.getInt(4),
                                calendar = c.getString(5).orEmpty(),
                                location = c.getString(6).orEmpty()
                            )
                        )
                    }
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())

        // all-day события хранятся в UTC — сравниваем по дате
        val todayUtcStart = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            val local = Calendar.getInstance()
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val allDayToday = all.filter { it.allDay && it.begin <= todayUtcStart && it.end > todayUtcStart }
        val timed = all.filter { !it.allDay && it.end > now }
        val timedToday = timed.count { it.begin <= endOfToday }

        Agenda(
            timed = timed.take(3),
            allDay = allDayToday.firstOrNull(),
            todayCount = timedToday + allDayToday.size
        )
    }
}

/** Следующий будильник из системных часов (Samsung Часы, Google Часы и др.). */
object AlarmRepo {
    fun next(ctx: Context): Long? =
        ctx.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
}
