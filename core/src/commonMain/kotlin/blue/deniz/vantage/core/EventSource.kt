package blue.deniz.vantage.core

import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.path
import io.ktor.http.takeFrom
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface EventSource {
    @Serializable
    @SerialName("unknown")
    data object Unknown : EventSource

    @Serializable
    @SerialName("local")
    data class Local(val isTestEvent: Boolean = false) : EventSource

    @Serializable
    sealed interface Remote : EventSource {
        val location: String
    }

    @Serializable
    @SerialName("http")
    data class Http(val url: String) : Remote {
        override val location get() = url
    }

    @Serializable
    @SerialName("at")
    data class At(val uri: AtUri) : Remote {
        override val location get() = uri.toString()
    }

    @Serializable
    @SerialName("folio")
    data class Folio(val id: String, val baseUrl: String = DEFAULT_BASE_URL) : Remote {
        override val location get() = URLBuilder(baseUrl).apply { path("events", "$id.evnt.json") }.buildString()

        fun editLink(editToken: String) = URLBuilder(location).apply { parameters.append("token", editToken) }.buildString()

        companion object {
            const val DEFAULT_BASE_URL = "https://folio.denizblue.workers.dev"
        }
    }

    @Serializable
    @SerialName("mediawiki")
    data class MediaWiki(val url: String, val title: String) : Remote {
        override val location
            get() = URLBuilder(url).apply {
                appendPathSegments("rest.php", "v1", "page")
                appendPathSegments(title, encodeSlash = true)
            }.buildString()
    }

    @Serializable
    @SerialName("webdav")
    data class WebDav(val url: String, val path: String) : Remote {
        override val location get() = URLBuilder(url).takeFrom(path).buildString()
    }

    @Serializable
    @SerialName("calendar")
    data class Calendar(val calendar: SubscribedCalendar, val uid: String) : EventSource
}

enum class EventSourceType {
    Unknown, Local, Http, At, Folio, MediaWiki, WebDav, Calendar;

    val serialName get() = name.lowercase()
}

val EventSource.type: EventSourceType
    get() = when (this) {
        EventSource.Unknown -> EventSourceType.Unknown
        is EventSource.Local -> EventSourceType.Local
        is EventSource.Http -> EventSourceType.Http
        is EventSource.At -> EventSourceType.At
        is EventSource.Folio -> EventSourceType.Folio
        is EventSource.MediaWiki -> EventSourceType.MediaWiki
        is EventSource.WebDav -> EventSourceType.WebDav
        is EventSource.Calendar -> EventSourceType.Calendar
    }

val EventSource.canUploadToFolio: Boolean
    get() = this is EventSource.Local && !isTestEvent
