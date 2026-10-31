package blue.deniz.vantage

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import blue.deniz.vantage.app.AndroidBackgroundEventChecks
import blue.deniz.vantage.app.AppState
import blue.deniz.vantage.app.AppStateOwner
import blue.deniz.vantage.app.EVENT_ID_EXTRA
import blue.deniz.vantage.app.VantageApp
import blue.deniz.vantage.app.createAppState

class VantageApplication : Application(), AppStateOwner {
    override val appState: AppState by lazy { createAppState(this, AndroidBackgroundEventChecks(this)) }
}

class MainActivity : ComponentActivity() {
    private val state get() = (application as VantageApplication).appState

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openIntent(intent)
        setContent { VantageApp(state) }
    }

    override fun onResume() {
        super.onResume()
        state.readClock()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openIntent(intent)
    }

    private fun openIntent(intent: Intent) {
        val eventId = intent.getStringExtra(EVENT_ID_EXTRA)
        when {
            eventId != null -> state.openEvent(eventId)
            intent.action == Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let(state::openSharedText)
            else -> intent.data?.let { state.openLink(it.toString()) }
        }
    }
}
