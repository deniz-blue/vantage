package blue.deniz.vantage.storage

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import blue.deniz.vantage.core.EventSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object MoveFolioEditTokensToCredentials : Migration(6, 7) {
    private class Credential(val id: String, val url: String, val secret: CredentialSecret, val updatedAt: Long)

    private class FolioEvent(val id: String, val source: EventSource.Folio, val editToken: String)

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun migrate(connection: SQLiteConnection) {
        val logins = connection.prepare("SELECT id, url, username, password, updatedAt FROM credentials").closing { select ->
            buildList {
                while (select.step()) {
                    add(Credential(select.getText(0), select.getText(1), CredentialSecret.Login(select.getText(2), select.getText(3)), select.getLong(4)))
                }
            }
        }
        val folioEvents = connection.prepare("SELECT id, source FROM events WHERE json_extract(source, '$.type') = 'folio' AND json_extract(source, '$.editToken') IS NOT NULL").closing { select ->
            buildList {
                while (select.step()) {
                    val text = select.getText(1)
                    val editToken = Json.parseToJsonElement(text).jsonObject["editToken"]?.jsonPrimitive?.contentOrNull ?: continue
                    val source = ColumnConverters.jsonToSource(text) as? EventSource.Folio ?: continue
                    add(FolioEvent(select.getText(0), source, editToken))
                }
            }
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val tokens = folioEvents.distinctBy { it.source.location }.map { event ->
            Credential(Uuid.random().toString(), event.source.location, CredentialSecret.Token(event.editToken), now)
        }

        connection.execSQL("DROP TABLE credentials")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `credentials` (`id` TEXT NOT NULL, `url` TEXT NOT NULL, `secret` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.prepare("INSERT INTO credentials (id, url, secret, updatedAt) VALUES (?, ?, ?, ?)").closing { insert ->
            for (credential in logins + tokens) {
                insert.bindText(1, credential.id)
                insert.bindText(2, credential.url)
                insert.bindText(3, ColumnConverters.secretToJson(credential.secret))
                insert.bindLong(4, credential.updatedAt)
                insert.step()
                insert.reset()
            }
        }
        connection.prepare("UPDATE events SET source = ? WHERE id = ?").closing { update ->
            for (event in folioEvents) {
                update.bindText(1, ColumnConverters.sourceToJson(event.source))
                update.bindText(2, event.id)
                update.step()
                update.reset()
            }
        }
    }
}
