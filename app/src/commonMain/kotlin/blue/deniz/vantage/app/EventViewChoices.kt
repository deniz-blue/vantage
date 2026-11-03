package blue.deniz.vantage.app

import blue.deniz.vantage.core.TimeFilter

enum class EventsView(val storedName: String, val label: String, val searches: Boolean, val filtersByTime: Boolean, val sorts: Boolean) {
    List("list", "List", searches = true, filtersByTime = false, sorts = true),
    Calendar("calendar", "Calendar", searches = false, filtersByTime = false, sorts = false),
    Map("map", "Map", searches = false, filtersByTime = true, sorts = false),
}

fun EventsView.timeShown(chosen: TimeFilter): TimeFilter = if (filtersByTime) chosen else TimeFilter.Future

enum class EventCardStyle(val storedName: String, val label: String) {
    Agenda("agenda", "Agenda"),
    Rows("list", "Rows"),
    Posters("posters", "Posters"),
    Tickets("tickets", "Tickets"),
}
