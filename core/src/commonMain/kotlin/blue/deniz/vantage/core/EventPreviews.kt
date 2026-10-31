package blue.deniz.vantage.core

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private val PREVIEW_FRESH_FOR = 10.minutes

class EventPreviews(private val resolver: EventResolver, clock: Clock = Clock.System) {
    private val resolved = ResultCache<EventSource.Remote, ResolvedEvent>(PREVIEW_FRESH_FOR, clock)

    fun lastResolved(link: ResolvedEvent): ResolvedEvent? = (link.source as? EventSource.Remote)?.let(resolved::last)

    suspend fun resolve(link: ResolvedEvent): ResolvedEvent {
        val source = link.source as? EventSource.Remote ?: return resolver.resolve(link)
        resolved.fresh(source)?.let { return it }
        return resolver.resolve(link).also { if (it.fetchError == null) resolved.store(source, it) }
    }
}
