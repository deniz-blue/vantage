package blue.deniz.vantage.storage

import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import blue.deniz.vantage.core.TimeRange
import blue.deniz.vantage.core.timeRanges

class RebuildEventTimeRanges : AutoMigrationSpec {
    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        val rangesByEvent = connection.prepare("SELECT eventId, data FROM event_cache WHERE data IS NOT NULL").closing { select ->
            buildMap {
                while (select.step()) {
                    val data = runCatching { ColumnConverters.jsonToEvent(select.getText(1)) }.getOrNull() ?: continue
                    put(select.getText(0), data.timeRanges())
                }
            }
        }
        for ((eventId, ranges) in rangesByEvent) connection.replaceTimeRanges(eventId, ranges)
    }
}

private suspend fun SQLiteConnection.replaceTimeRanges(eventId: String, ranges: List<TimeRange>) {
    prepare("DELETE FROM event_time_ranges WHERE eventId = ?").closing { delete ->
        delete.bindText(1, eventId)
        delete.step()
    }
    prepare("INSERT OR IGNORE INTO event_time_ranges (eventId, startsAt, endsAt) VALUES (?, ?, ?)").closing { insert ->
        for (range in ranges) {
            insert.bindText(1, eventId)
            insert.bindLong(2, range.start.toEpochMilliseconds())
            insert.bindLong(3, range.end.toEpochMilliseconds())
            insert.step()
            insert.reset()
        }
    }
}

private inline fun <T> SQLiteStatement.closing(block: (SQLiteStatement) -> T): T =
    try {
        block(this)
    } finally {
        close()
    }
