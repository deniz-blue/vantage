package blue.deniz.vantage.core

import directory.evnt.Component

data class RelatedEvent(val type: String, val target: String, val event: ResolvedEvent?)

fun ResolvedEvent.isAddressedBy(target: String): Boolean =
    data?.id == target || (source as? EventSource.Remote)?.location == target

fun List<ResolvedEvent>.addressedBy(target: String): ResolvedEvent? = firstOrNull { it.isAddressedBy(target) }

fun ResolvedEvent.relations(): List<Component.Relation> = data?.components.orEmpty().filterIsInstance<Component.Relation>()

fun ResolvedEvent.relatedEvents(library: List<ResolvedEvent>): List<RelatedEvent> {
    val stated = relations().map { RelatedEvent(it.type, it.target, library.addressedBy(it.target)) }
    val statedEvents = stated.mapNotNull { it.event?.id }.toSet()
    val pointingHere = library
        .filter { other -> other.id != id && other.id !in statedEvents }
        .flatMap { other ->
            other.relations()
                .filter { isAddressedBy(it.target) }
                .mapNotNull { relation -> Component.Relation.reverseOf(relation.type)?.let { RelatedEvent(it, other.data?.id.orEmpty(), other) } }
        }
    return stated + pointingHere
}
