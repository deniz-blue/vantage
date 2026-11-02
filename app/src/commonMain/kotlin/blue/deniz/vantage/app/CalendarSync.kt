package blue.deniz.vantage.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import blue.deniz.vantage.app.screens.LoadState
import blue.deniz.vantage.app.screens.loadCatching
import blue.deniz.vantage.core.DeviceCalendars
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.CalendarLinkReader
import blue.deniz.vantage.core.SubscribedCalendar
import blue.deniz.vantage.storage.EventStore
import blue.deniz.vantage.storage.ForgottenCalendar
import directory.evnt.convert.ConvertOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

private const val SYNCED_DAYS_BEFORE_TODAY = 180
private const val SYNCED_DAYS_AFTER_TODAY = 400

class RemovedSubscription(val position: Int, val forgotten: ForgottenCalendar)

@Stable
class CalendarSync(
    private val store: EventStore,
    private val links: CalendarLinkReader,
    val deviceCalendars: DeviceCalendars?,
    private val settings: AppSettings,
    private val clock: Clock,
) {
    var syncing by mutableStateOf(false)
        private set
    var lastSyncedAt by mutableStateOf<Instant?>(null)
        private set
    val failures = mutableStateMapOf<SubscribedCalendar, String>()
    val linkNames = mutableStateMapOf<SubscribedCalendar.Link, String>()
    private val deviceCalendarColors = mutableStateMapOf<SubscribedCalendar.OnDevice, Int>()

    private val scope = MainScope()
    private val running = Mutex()

    fun syncAll() {
        scope.launch { syncing { settings.subscribedCalendars.forEach { sync(it) } } }
    }

    fun subscribe(calendar: SubscribedCalendar) {
        if (calendar in settings.subscribedCalendars) return
        settings.changeSubscribedCalendars(settings.subscribedCalendars + calendar)
        scope.launch { syncing { sync(calendar) } }
    }

    fun unsubscribe(calendar: SubscribedCalendar, onForgotten: (RemovedSubscription) -> Unit = {}) {
        val position = settings.subscribedCalendars.indexOf(calendar)
        settings.changeSubscribedCalendars(settings.subscribedCalendars - calendar)
        failures.remove(calendar)
        scope.launch { onForgotten(RemovedSubscription(position, running.withLock { store.forgetCalendar(calendar) })) }
    }

    suspend fun restore(removed: RemovedSubscription) {
        val calendar = removed.forgotten.calendar
        if (calendar in settings.subscribedCalendars) return
        running.withLock { store.restoreCalendar(removed.forgotten) }
        val subscribed = settings.subscribedCalendars
        settings.changeSubscribedCalendars(subscribed.toMutableList().apply { add(removed.position.coerceIn(0, subscribed.size), calendar) })
        syncing { sync(calendar) }
    }

    fun unhide(source: EventSource.Calendar) {
        scope.launch {
            syncing {
                store.unhideCalendarEvent(source)
                if (source.calendar in settings.subscribedCalendars) sync(source.calendar)
            }
        }
    }

    fun colorOf(source: EventSource): Int? = ((source as? EventSource.Calendar)?.calendar as? SubscribedCalendar.OnDevice)?.let { deviceCalendarColors[it] }

    private suspend fun syncing(block: suspend () -> Unit) = running.withLock {
        syncing = true
        try {
            block()
            lastSyncedAt = clock.now()
        } finally {
            syncing = false
        }
    }

    private suspend fun sync(calendar: SubscribedCalendar) {
        val zone = settings.timeZone
        val today = clock.todayIn(zone)
        val within = today.minus(SYNCED_DAYS_BEFORE_TODAY, DateTimeUnit.DAY)..today.plus(SYNCED_DAYS_AFTER_TODAY, DateTimeUnit.DAY)
        val options = ConvertOptions(language = settings.contentLanguage, timeZoneId = zone.id)
        val loaded = loadCatching {
            when (calendar) {
                is SubscribedCalendar.Link -> withContext(Dispatchers.Default) { links.read(calendar, within, options) }
                    .also { contents -> contents.name?.let { linkNames[calendar] = it } }
                    .entries
                is SubscribedCalendar.OnDevice -> {
                    val device = deviceCalendars?.takeIf { it.hasPermission() } ?: return@loadCatching null
                    device.list().firstOrNull { it.id == calendar.calendarId }?.color?.let { deviceCalendarColors[calendar] = it }
                    device.read(
                        calendar.calendarId,
                        within.start.atStartOfDayIn(zone),
                        within.endInclusive.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone),
                        options,
                    )
                }
            }?.let { entries -> store.syncCalendar(calendar, entries) }
        }
        if (loaded is LoadState.Failed) failures[calendar] = loaded.message else failures.remove(calendar)
    }
}
