package blue.deniz.vantage.core

import directory.evnt.OpenEvnt
import directory.evnt.convert.CommunityLexicon
import io.ktor.http.Url
import io.ktor.http.decodeURLPart
import io.ktor.http.parseUrl
import io.ktor.http.protocolWithAuthority

internal val EVENT_COLLECTIONS = mapOf(
    OpenEvnt.AT_RECORD_TYPE to EventFormat.OpenEvnt,
    CommunityLexicon.EVENT_TYPE to EventFormat.CommunityLexicon,
)

private val FOLIO_HOST = Url(EventSource.Folio.DEFAULT_BASE_URL).host

private val MEDIAWIKI_PAGE_PATTERN = Regex("""https?://(.*)/rest\.php/v1/page/(.*)""")

internal fun isHttpUrl(text: String) = text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)

internal fun recognizeLink(text: String): ResolvedEvent? = recognizeAtUri(text) ?: recognizeHttpUrl(text)

private fun recognizeAtUri(text: String): ResolvedEvent? {
    if (!text.startsWith("at://")) return null
    val uri = AtUri.parseOrNull(text) ?: return null
    val format = EVENT_COLLECTIONS[uri.collection] ?: return null
    return ResolvedEvent(source = EventSource.At(uri), format = format)
}

private fun recognizeHttpUrl(text: String): ResolvedEvent? {
    if (!isHttpUrl(text)) return null
    val url = parseUrl(text) ?: return null
    if (url.toString().startsWith(EventsLink.BASE_URL) || url.host == AppSchemes.WEB_HOST) return recognizeEventsLink(url)
    return recognizeFolio(url)
        ?: recognizeMediaWiki(text)
        ?: ResolvedEvent(source = EventSource.Http(text), format = formatFromExtension(url))
}

private fun recognizeFolio(url: Url): ResolvedEvent? {
    if (url.host != FOLIO_HOST) return null
    val segments = url.segments
    if (segments.getOrNull(0) != "events") return null
    val id = segments.getOrNull(1)?.removeSuffix(".evnt.json")?.removeSuffix(".json") ?: return null
    return ResolvedEvent(
        source = EventSource.Folio(id = id, baseUrl = url.protocolWithAuthority, editToken = url.parameters["token"]),
        format = EventFormat.OpenEvnt,
    )
}

private fun recognizeMediaWiki(text: String): ResolvedEvent? {
    val (site, title) = MEDIAWIKI_PAGE_PATTERN.matchEntire(text)?.destructured ?: return null
    return ResolvedEvent(
        source = EventSource.MediaWiki(url = "https://$site/", title = title.decodeURLPart()),
        format = EventFormat.OpenEvnt,
    )
}

private fun recognizeEventsLink(url: Url): ResolvedEvent? = EventsLink.parseIntent(url)?.let(::recognizeEventsLinkIntent)

internal fun recognizeEventsLinkIntent(intent: EventsLink.Intent): ResolvedEvent? = when {
    intent.at != null -> recognizeAtUri(intent.at)
    intent.url != null -> recognizeLink(intent.url)
    intent.data != null -> ResolvedEvent(source = EventSource.Local(), format = EventFormat.OpenEvnt, raw = intent.data)
    else -> null
}

private fun formatFromExtension(url: Url) = when {
    url.encodedPath.endsWith(".json") -> EventFormat.OpenEvnt
    url.encodedPath.endsWith(".ics") -> EventFormat.ICalendar
    else -> EventFormat.Unknown
}

private const val LINK_OPENERS = "<([{\"'"
private const val LINK_CLOSERS = ">)]}\"'.,;:!?"

fun findEventLink(text: String): String? =
    text.split(Regex("""\s+"""))
        .map { it.trimStart { char -> char in LINK_OPENERS }.trimEnd { char -> char in LINK_CLOSERS } }
        .firstOrNull { recognizeLink(it) != null }
