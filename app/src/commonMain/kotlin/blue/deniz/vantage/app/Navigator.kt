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
    data class Feed(val feed: EventFeed) : Route
    data class CreateEvent(val sharedText: String? = null, val day: LocalDate? = null, val draft: OpenEvnt? = null) : Form
    data class EditEvent(val id: String, val draft: OpenEvnt? = null) : Form
    data class EditFromLink(val request: EventsLink.EditRequest) : Form
    data object AppearanceSettings : Route
    data object NotificationSettings : Route
    data object BackupSettings : Route
    data object Setup : Route
    data object HomeSettings : Route
    data object TagSettings : Route
    data class CalendarSettings(val offeredLink: String? = null) : Route
    data object About : Route
    data object DeveloperTools : Route
}

data class OpenedScreen(val route: Route, val openingId: Long)

@Stable
class Navigator(start: Route.Tab = Route.Home) {
    private var openings = 0L
    private val backStack = mutableStateListOf(opening(start))
    private val clearActions = mutableMapOf<Int, () -> Unit>()

    val screens: List<OpenedScreen> get() = backStack
    val entries: List<Route> get() = backStack.map { it.route }
    val current: Route get() = backStack.last().route
    val currentTab: Route.Tab get() = backStack.map { it.route }.filterIsInstance<Route.Tab>().last()
    val canGoBack: Boolean get() = backStack.size > 1

    private fun opening(route: Route) = OpenedScreen(route, ++openings)

    fun open(route: Route) {
        backStack += opening(route)
    }

    fun replace(route: Route) {
        clearActions -= backStack.lastIndex
        backStack[backStack.lastIndex] = opening(route)
    }

    fun openDraft(form: Route.Form) {
        if (current.isSameFormAs(form)) replace(form) else open(form)
    }

    private fun Route.isSameFormAs(form: Route.Form) = when (form) {
        is Route.CreateEvent -> this is Route.CreateEvent
        is Route.EditEvent -> this is Route.EditEvent && id == form.id
        is Route.EditFromLink -> this is Route.EditFromLink && request == form.request
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
        backStack += opening(tab)
    }
}
