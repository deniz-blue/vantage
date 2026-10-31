package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import kotlin.time.Instant

data class ResolvedEvent(
    val id: String? = null,
    val source: EventSource = EventSource.Unknown,
    val format: EventFormat = EventFormat.Unknown,
    val raw: String? = null,
    val data: OpenEvnt? = null,
    val fetchError: EventError? = null,
    val parseError: EventError? = null,
    val revision: Revision = Revision(),
    val updatedAt: Instant? = null,
) {
    val error get() = fetchError ?: parseError

    companion object {
        fun fromLink(text: String): ResolvedEvent? = recognizeLink(text.trim())

        fun fromEventsLinkIntent(intent: EventsLink.Intent): ResolvedEvent? = recognizeEventsLinkIntent(intent)
    }
}
