package blue.deniz.vantage.core

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url

object AppSchemes {
    const val WEB_HOST = "vantage.tsx.lt"
    val SCHEMES = setOf("vantage", "evnt", "web+evnt")

    fun webAddressOf(url: Url): Url {
        if (url.protocol.name !in SCHEMES) return url
        return URLBuilder(url).apply {
            protocol = URLProtocol.HTTPS
            host = WEB_HOST
            port = 0
            pathSegments = listOf(url.host).filter { it.isNotEmpty() } + url.segments
        }.build()
    }
}
