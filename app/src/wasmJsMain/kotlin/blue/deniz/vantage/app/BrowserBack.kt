package blue.deniz.vantage.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window

private fun pushBackEntry(url: String?) {
    js("history.pushState('vantage-back', '', url)")
}

private fun isOnBackEntry(): Boolean = js("history.state === 'vantage-back'")

private fun replaceBackEntryAddress(url: String) {
    js("history.replaceState('vantage-back', '', url)")
}

private fun replaceBaseEntryAddress(url: String) {
    js("history.replaceState(null, '', url)")
}

object BrowserBackDispatcher {
    class Handler(val handlesEscape: Boolean, val onBack: () -> Unit) {
        var enabled = false
            set(value) {
                if (field == value) return
                field = value
                if (value && handlers.remove(this)) handlers += this
                updateBackEntry()
            }
    }

    private val handlers = mutableListOf<Handler>()
    private var backEntryPushed = isOnBackEntry()
    private var droppingBackEntry = false
    private var baseAddress: String? = null
    private var topAddress: String? = null

    init {
        window.addEventListener("popstate", { onPopState() })
    }

    fun add(handler: Handler) {
        handlers += handler
        updateBackEntry()
    }

    fun remove(handler: Handler) {
        handlers -= handler
        updateBackEntry()
    }

    fun backFromEscape(): Boolean {
        val top = handlers.lastOrNull { it.enabled } ?: return false
        if (!top.handlesEscape || !backEntryPushed || droppingBackEntry) return false
        window.history.back()
        return true
    }

    fun showAddresses(base: String?, top: String?) {
        baseAddress = base ?: baseAddress
        topAddress = top ?: topAddress
        if (droppingBackEntry) return
        if (backEntryPushed) topAddress?.let(::replaceBackEntryAddress) else baseAddress?.let(::replaceBaseEntryAddress)
    }

    private fun updateBackEntry() {
        if (droppingBackEntry) return
        val wanted = handlers.any { it.enabled }
        if (wanted && !backEntryPushed) {
            pushBackEntry(topAddress)
            backEntryPushed = true
        } else if (!wanted && backEntryPushed) {
            droppingBackEntry = true
            window.history.back()
        }
    }

    private fun onPopState() {
        val dropped = droppingBackEntry
        droppingBackEntry = false
        backEntryPushed = isOnBackEntry()
        if (!backEntryPushed) {
            baseAddress?.let(::replaceBaseEntryAddress)
            if (!dropped) handlers.lastOrNull { it.enabled }?.onBack()
        }
        updateBackEntry()
    }
}

actual fun handleEscapeKey(): Boolean = BrowserBackDispatcher.backFromEscape()

@Composable
actual fun SystemBackHandler(enabled: Boolean, handlesEscape: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    val handler = remember(handlesEscape) { BrowserBackDispatcher.Handler(handlesEscape) { currentOnBack() } }
    DisposableEffect(handler) {
        BrowserBackDispatcher.add(handler)
        onDispose { BrowserBackDispatcher.remove(handler) }
    }
    SideEffect { handler.enabled = enabled }
}
