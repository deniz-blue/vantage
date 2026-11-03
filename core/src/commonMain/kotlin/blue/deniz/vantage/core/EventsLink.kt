package blue.deniz.vantage.core

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.formUrlEncode
import io.ktor.http.path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object EventsLink {
    const val BASE_URL = "https://eventsl.ink/"

    data class Intent(val at: String? = null, val url: String? = null, val data: String? = null)

    fun parseIntent(url: Url): Intent? {
        val isEvent = url.encodedPath == "/e" || url.encodedPath == "/event" || url.parameters["type"] == "event"
        if (!isEvent) return null
        return Intent(at = url.parameters["at"], url = url.parameters["url"], data = url.parameters["data"])
    }

    data class EditRequest(val event: Intent, val returnUrl: String, val state: String? = null) {
        val returnHost: String get() = Url(returnUrl).host
    }

    sealed interface EditReply {
        val request: EditRequest

        data class Saved(override val request: EditRequest, val eventJson: String) : EditReply

        data class Cancelled(override val request: EditRequest) : EditReply
    }

    fun parseEditRequest(url: Url): EditRequest? {
        val isEdit = url.encodedPath == "/edit" || url.parameters["type"] == "edit"
        if (!isEdit) return null
        val returnUrl = url.parameters["return"]?.takeIf(::isHttpUrl) ?: return null
        return EditRequest(
            Intent(at = url.parameters["at"], url = url.parameters["url"], data = url.parameters["data"]),
            returnUrl,
            url.parameters["state"],
        )
    }

    fun returnLinkFor(reply: EditReply): String {
        val answer = when (reply) {
            is EditReply.Saved -> "event" to reply.eventJson
            is EditReply.Cancelled -> "cancelled" to "true"
        }
        return reply.request.returnUrl.substringBefore('#') + "#" + listOf(answer, "state" to reply.request.state).filter { it.second != null }.formUrlEncode()
    }

    fun replyMessageFor(reply: EditReply): String = buildJsonObject {
        put("type", EDIT_REPLY_MESSAGE_TYPE)
        when (reply) {
            is EditReply.Saved -> put("event", Json.parseToJsonElement(reply.eventJson))
            is EditReply.Cancelled -> put("cancelled", true)
        }
        reply.request.state?.let { put("state", it) }
    }.toString()

    private const val EDIT_REPLY_MESSAGE_TYPE = "evnt-edit"

    fun parseIndexLink(url: Url): EventFeed.OpenEvntIndex? {
        val isIndex = url.encodedPath == "/index" || url.parameters["type"] == "index"
        if (!isIndex) return null
        return url.parameters["url"]?.takeIf(::isHttpUrl)?.let(EventFeed::OpenEvntIndex)
    }

    fun intentFor(source: EventSource): Intent? = when (source) {
        is EventSource.At -> Intent(at = source.location)
        is EventSource.Http, is EventSource.Folio, is EventSource.MediaWiki -> Intent(url = source.location)
        is EventSource.WebDav, is EventSource.Calendar, is EventSource.Local, EventSource.Unknown -> null
    }

    fun shareLinkTo(source: EventSource): String? {
        val intent = intentFor(source) ?: return null
        return intent.at?.let { linkWith("at", it) } ?: intent.url?.let { linkWith("url", it) }
    }

    fun shareLinkTo(feed: EventFeed): String? = when (feed) {
        is EventFeed.OpenEvntIndex -> URLBuilder(BASE_URL).apply {
            path("index")
            parameters.append("url", feed.url)
        }.buildString()
        is EventFeed.AtRepository -> null
    }

    private fun linkWith(name: String, value: String) =
        URLBuilder(BASE_URL).apply {
            path("e")
            parameters.append(name, value)
        }.buildString()
}
