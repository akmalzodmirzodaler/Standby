package tj.mirzodaler.standby.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int
)

/** Ближайшие события из всех календарей телефона (Samsung, Google, Outlook — что синхронизировано). */
object CalendarRepo {
    fun hasPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** null — нет разрешения; пустой список — событий нет. */
    suspend fun upcoming(ctx: Context, limit: Int = 4): List<CalEvent>? =
        withContext(Dispatchers.IO) {
            if (!hasPermission(ctx)) return@withContext null
            val now = System.currentTimeMillis()
            val until = now + 48L * 60 * 60 * 1000
            val uri = Instances.CONTENT_URI.buildUpon().also {
                ContentUris.appendId(it, now)
                ContentUris.appendId(it, until)
            }.build()
            val projection = arrayOf(
                Instances.TITLE, Instances.BEGIN, Instances.END,
                Instances.ALL_DAY, Instances.DISPLAY_COLOR
            )
            runCatching {
                ctx.contentResolver.query(uri, projection, null, null, "${Instances.BEGIN} ASC")
                    ?.use { c ->
                        buildList {
                            while (c.moveToNext() && size < limit) {
                                val end = c.getLong(2)
                                if (end < now) continue
                                add(
                                    CalEvent(
                                        title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "Без названия",
                                        begin = c.getLong(1),
                                        end = end,
                                        allDay = c.getInt(3) == 1,
                                        color = c.getInt(4)
                                    )
                                )
                            }
                        }
                    } ?: emptyList()
            }.getOrDefault(emptyList())
        }
}
