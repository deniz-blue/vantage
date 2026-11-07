package blue.deniz.vantage.app.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import blue.deniz.vantage.app.collectAcrossSession
import blue.deniz.vantage.core.EventCollection
import blue.deniz.vantage.core.EventListing
import blue.deniz.vantage.core.EventListingStatus
import blue.deniz.vantage.core.EventQuery

private const val PAGE_SIZE = 50
private const val LOAD_MORE_DISTANCE = 10

@Composable
fun rememberPagedListing(sessionKey: String, events: EventCollection, query: EventQuery): PagedListing {
    var limit by rememberSaveable(events, query) { mutableIntStateOf(PAGE_SIZE) }
    var attempt by remember { mutableIntStateOf(0) }
    val listing = collectAcrossSession(sessionKey, listOf(events, query, limit, attempt)) { events.list(query, limit) }
    return PagedListing(listing, limit, loadMore = { limit += PAGE_SIZE }, retry = { attempt++ })
}

class PagedListing(val listing: EventListing?, val limit: Int, val loadMore: () -> Unit, val retry: () -> Unit) {
    val events get() = listing?.events.orEmpty()
    val wantsMore get() = listing?.status == EventListingStatus.HasMore && events.size >= limit
    val isEmpty get() = listing != null && events.isEmpty() && (listing.status == EventListingStatus.Complete || listing.status == EventListingStatus.HasMore)
}

@Composable
fun LoadMoreNearTheEnd(paged: PagedListing, itemsAfterLastVisible: () -> Int) {
    val current by rememberUpdatedState(paged)
    LaunchedEffect(Unit) {
        snapshotFlow { current.wantsMore && itemsAfterLastVisible() <= LOAD_MORE_DISTANCE }.collect { nearTheEnd ->
            if (nearTheEnd) current.loadMore()
        }
    }
}
