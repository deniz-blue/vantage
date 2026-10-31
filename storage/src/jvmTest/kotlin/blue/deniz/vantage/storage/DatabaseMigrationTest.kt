package blue.deniz.vantage.storage

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import blue.deniz.vantage.core.EventFormat
import blue.deniz.vantage.core.EventSource
import blue.deniz.vantage.core.Revision
import directory.evnt.Occurrence
import directory.evnt.OpenEvnt
import directory.evnt.PartialDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DatabaseMigrationTest {
    private fun createVersion(version: Int, path: String) {
        val schema = Json.parseToJsonElement(File("schemas/blue.deniz.vantage.storage.VantageDatabase/$version.json").readText()).jsonObject["database"]!!.jsonObject
        val connection = BundledSQLiteDriver().open(path)
        for (entity in schema["entities"]!!.jsonArray.map { it.jsonObject }) {
            val table = entity["tableName"]!!.jsonPrimitive.content
            connection.execSQL(entity["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray?.forEach { index ->
                connection.execSQL(index.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
        schema["setupQueries"]!!.jsonArray.forEach { connection.execSQL(it.jsonPrimitive.content) }
        connection.execSQL("PRAGMA user_version = $version")
        connection.close()
    }

    private fun createFirstVersion(path: String) {
        createVersion(1, path)
        BundledSQLiteDriver().open(path).apply {
            execSQL("INSERT INTO tags (id, name, color, updatedAt) VALUES ('t1', 'Anime', NULL, 0)")
            close()
        }
    }

    private fun day(day: Int) = PartialDate.Day(LocalDate(2026, 11, day), "Europe/Vilnius")

    private fun String.quoted() = "'" + replace("'", "''") + "'"

    @Test
    fun movingToVersionFiveDropsCancelledDatesFromTheTimeIndex() = runTest {
        val file = File.createTempFile("vantage-v4", ".db").apply { delete() }
        val cancelled = Occurrence(start = day(4), status = Occurrence.Status.Cancelled)
        val held = Occurrence(start = day(11))
        val event = OpenEvnt(id = "urn:uuid:test", name = mapOf("en" to "Weekly"), occurrences = listOf(cancelled, held))
        try {
            createVersion(4, file.path)
            BundledSQLiteDriver().open(file.path).apply {
                execSQL("INSERT INTO events (id, source, format, addedAt) VALUES ('e1', ${ColumnConverters.sourceToJson(EventSource.Local()).quoted()}, ${ColumnConverters.formatToName(EventFormat.OpenEvnt).quoted()}, 0)")
                execSQL("INSERT INTO event_cache (eventId, data, revision) VALUES ('e1', ${ColumnConverters.eventToJson(event).quoted()}, ${ColumnConverters.revisionToJson(Revision()).quoted()})")
                for (occurrence in listOf(cancelled, held)) {
                    val start = occurrence.start!!
                    execSQL("INSERT INTO event_time_ranges (eventId, startsAt, endsAt) VALUES ('e1', ${start.startInstant.toEpochMilliseconds()}, ${start.endInstant.toEpochMilliseconds()})")
                }
                close()
            }
            val database = Room.databaseBuilder<VantageDatabase>(file.path)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            val starts = database.events().timeRangesOf("e1").map { it.startsAt }
            assertEquals(listOf(Instant.parse("2026-11-10T22:00:00Z")), starts)
            database.close()
        } finally {
            file.delete()
        }
    }

    @Test
    fun opensADatabaseFromTheFirstVersion() = runTest {
        val file = File.createTempFile("vantage-v1", ".db").apply { delete() }
        try {
            createFirstVersion(file.path)
            val database = Room.databaseBuilder<VantageDatabase>(file.path)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            assertEquals(listOf("Anime"), database.tags().observeAll().first().map { it.name })
            assertEquals(emptyList(), database.events().observeSeenAndCurrentEvents().first())
            database.close()
        } finally {
            file.delete()
        }
    }
}
