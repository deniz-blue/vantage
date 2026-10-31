package blue.deniz.vantage.app

enum class EventsView(val storedName: String, val label: String, val searches: Boolean, val filtersByTime: Boolean, val sorts: Boolean) {
    List("list", "List", searches = true, filtersByTime = true, sorts = true),
    Calendar("calendar", "Calendar", searches = false, filtersByTime = false, sorts = false),
    Map("map", "Map", searches = false, filtersByTime = true, sorts = false),
}

enum class EventCardStyle(val storedName: String, val label: String) {
    Agenda("agenda", "Agenda"),
    Rows("list", "Rows"),
    Posters("posters", "Posters"),
    Tickets("tickets", "Tickets"),
}
