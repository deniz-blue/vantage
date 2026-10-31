package blue.deniz.vantage.core

import directory.evnt.Translations
import kotlinx.coroutines.flow.Flow

interface EventCollection {
    val orders: List<EventOrder>

    fun list(query: EventQuery, limit: Int): Flow<EventListing>
}

data class EventListing(
    val events: List<ResolvedEvent>,
    val status: EventListingStatus,
    val listedNames: Map<EventSource, Translations> = emptyMap(),
    val count: EventCount? = null,
)

sealed interface EventListingStatus {
    data class Loading(val progress: Float? = null) : EventListingStatus
    data object HasMore : EventListingStatus
    data object Complete : EventListingStatus
    data class Failed(val message: String) : EventListingStatus
}

sealed interface EventCount {
    data class Total(val events: Int) : EventCount
    data class Matching(val matching: Int, val read: Int, val readWholeSource: Boolean) : EventCount
}
