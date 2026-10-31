package blue.deniz.vantage.storage

import blue.deniz.vantage.core.EventCollection
import blue.deniz.vantage.core.EventListing
import blue.deniz.vantage.core.EventListingStatus
import blue.deniz.vantage.core.EventOrder
import blue.deniz.vantage.core.EventQuery
import kotlinx.coroutines.flow.map

fun EventStore.eventsMatching(filter: EventListQuery = EventListQuery()): EventCollection = StoredEvents(this, filter)

private data class StoredEvents(val store: EventStore, val filter: EventListQuery) : EventCollection {
    override val orders = listOf(EventOrder.Start, EventOrder.Name)

    override fun list(query: EventQuery, limit: Int) = store.observeList(filter.narrowedTo(query, limit)).map { events ->
        EventListing(events, if (events.size >= limit) EventListingStatus.HasMore else EventListingStatus.Complete)
    }
}

fun EventListQuery.narrowedTo(query: EventQuery, limit: Int?) = copy(
    search = query.search.takeIf { it.isNotBlank() } ?: search,
    from = query.from ?: from,
    until = query.until ?: until,
    area = query.area ?: area,
    order = when (query.order) {
        EventOrder.Listed -> EventListQuery.Order.RecentlyAdded
        EventOrder.Start -> EventListQuery.Order.Start
        EventOrder.Name -> EventListQuery.Order.Name
    },
    limit = limit,
)
