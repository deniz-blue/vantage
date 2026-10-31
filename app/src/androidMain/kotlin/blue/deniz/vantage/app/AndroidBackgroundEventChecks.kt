package blue.deniz.vantage.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import blue.deniz.vantage.app.ui.describe
import blue.deniz.vantage.storage.ChangedEvent
import blue.deniz.vantage.core.SHORTEST_REFRESH_INTERVAL
import directory.evnt.translate
import java.util.concurrent.TimeUnit

const val EVENT_ID_EXTRA = "blue.deniz.vantage.EVENT_ID"
private const val EVENT_CHECKS_WORK = "event-checks"
private const val EVENT_CHANGES_CHANNEL = "event-changes"

interface AppStateOwner {
    val appState: AppState
}

class AndroidBackgroundEventChecks(private val context: Context) : BackgroundEventChecks {
    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<EventChecksWorker>(SHORTEST_REFRESH_INTERVAL.inWholeMinutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(EVENT_CHECKS_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(EVENT_CHECKS_WORK)
    }
}

class EventChecksWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val state = (applicationContext as AppStateOwner).appState
        val changed = state.store.refreshDueRemoteEvents()
        val notifications = applicationContext.getSystemService(NotificationManager::class.java)
        if (changed.isNotEmpty() && notifications.areNotificationsEnabled()) {
            notifications.createNotificationChannel(
                NotificationChannel(EVENT_CHANGES_CHANNEL, "Event Changes", NotificationManager.IMPORTANCE_DEFAULT),
            )
            changed.forEach { notifications.notify(it.id, 0, changeNotification(state, it)) }
        }
        return Result.success()
    }

    private fun changeNotification(state: AppState, event: ChangedEvent): Notification {
        val formatting = state.formatting
        val lines = event.differences.map { formatting.describe(it, state.preferredLanguages) }
        val open = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)!!
            .putExtra(EVENT_ID_EXTRA, event.id)
        val tap = PendingIntent.getActivity(
            applicationContext,
            event.id.hashCode(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(applicationContext, EVENT_CHANGES_CHANNEL)
            .setSmallIcon(R.drawable.event_change_notification)
            .setContentTitle(event.data.name.translate(state.preferredLanguages))
            .setContentText(lines.first())
            .setStyle(Notification.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
    }
}
