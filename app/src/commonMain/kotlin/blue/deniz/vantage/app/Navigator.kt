package blue.deniz.vantage.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import blue.deniz.vantage.core.EventFeed
import blue.deniz.vantage.core.EventsLink
import directory.evnt.OpenEvnt
import kotlinx.datetime.LocalDate

sealed interface Route {
    sealed interface Tab : Route
    sealed interface Form : Route
    data object Home : Tab
    data object MyEvents : Tab
    data object Settings : Tab
    data class EventDetails(val id: String, val openedFromCard: Boolean = false) : Route
    data class EventFromLink(val intent: EventsLink.Intent) : Route
    data object ChooseFeed : Route
    data class Feed(val feed: EventFeed) : Route
    data class CreateEvent(val sharedText: String? = null, val day: LocalDate? = null, val draft: OpenEvnt? = null) : Form
    data class EditEvent(val id: String, val draft: OpenEvnt? = null) : Form
    data class EditFromLink(val request: EventsLink.EditRequest) : Form
    data object HomeSettings : Route
    data object TagSettings : Route
    data object CalendarSettings : Route
    data object About : Route
    data object DeveloperTools : Route
}

@Stable
class Navigator(start: Route.Tab = Route.Home) {
    private val backStack = mutableStateListOf<Route>(start)
    private val clearActions = mutableMapOf<Int, () -> Unit>()

    val entries: List<Route> get() = backStack
    val current: Route get() = backStack.last()
    val currentTab: Route.Tab get() = backStack.filterIsInstance<Route.Tab>().last()
    val canGoBack: Boolean get() = backStack.size > 1

    fun open(route: Route) {
        backStack += route
    }

    fun replace(route: Route) {
        clearActions -= backStack.lastIndex
        backStack[backStack.lastIndex] = route
    }

    fun goBack(): Boolean {
        if (!canGoBack) return false
        clearActions -= backStack.lastIndex
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun setClearAction(depth: Int, action: () -> Unit) {
        clearActions[depth] = action
    }

    fun switchTab(tab: Route.Tab) {
        val actions = clearActions.values.toList()
        clearActions.clear()
        actions.forEach { it() }
        backStack.clear()
        backStack += tab
    }
}
