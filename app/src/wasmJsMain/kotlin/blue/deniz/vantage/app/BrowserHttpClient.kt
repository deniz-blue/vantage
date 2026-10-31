package blue.deniz.vantage.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.engine.js.JsError
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import kotlinx.io.IOException

// Ktor's browser engine reports a failed fetch as an Error caused by a JsError, neither of which is an Exception.
fun browserHttpClient(): HttpClient = HttpClient(Js).also { client ->
    client.plugin(HttpSend).intercept { request ->
        try {
            execute(request)
        } catch (failed: Throwable) {
            if (failed !is JsError && failed.cause !is JsError) throw failed
            throw IOException("Could not reach ${request.url.host}", failed)
        }
    }
}
