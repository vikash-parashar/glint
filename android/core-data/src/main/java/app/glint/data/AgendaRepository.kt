package app.glint.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NextEvent(val title: String, val beginMillis: Long, val allDay: Boolean)

/** Reads upcoming events. Returns empty until READ_CALENDAR is granted. */
class AgendaRepository(private val context: Context) {
    fun hasPermission() =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun next(now: Long = System.currentTimeMillis()): NextEvent? = upcoming(now, 1).firstOrNull()

    suspend fun upcoming(now: Long = System.currentTimeMillis(), limit: Int = 4, hours: Int = 24): List<NextEvent> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
                ContentUris.appendId(it, now - 15 * 60_000)
                ContentUris.appendId(it, now + hours * 3600_000L)
            }.build()
            val cols = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY)
            val out = ArrayList<NextEvent>()
            context.contentResolver.query(uri, cols, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    out += NextEvent(c.getString(0) ?: "Event", c.getLong(1), c.getInt(2) == 1)
                }
            }
            out
        }

    /** Day-of-month numbers in this month that have at least one event (for month grids). */
    suspend fun busyDaysThisMonth(): Set<Int> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptySet()
        val today = java.time.LocalDate.now()
        val zone = java.time.ZoneId.systemDefault()
        val start = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.withDayOfMonth(today.lengthOfMonth()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start); ContentUris.appendId(it, end)
        }.build()
        val days = HashSet<Int>()
        context.contentResolver.query(uri, arrayOf(CalendarContract.Instances.BEGIN), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                days += java.time.Instant.ofEpochMilli(c.getLong(0)).atZone(zone).dayOfMonth
            }
        }
        days
    }
}
