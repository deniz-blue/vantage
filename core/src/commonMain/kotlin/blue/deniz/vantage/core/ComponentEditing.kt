package blue.deniz.vantage.core

import directory.evnt.Component

fun List<Component>.withLinkMoved(fromLinkIndex: Int, toLinkIndex: Int): List<Component> {
    val linkSlots = indices.filter { this[it] is Component.Link }
    if (fromLinkIndex !in linkSlots.indices || toLinkIndex !in linkSlots.indices) return this
    val links = linkSlots.map { this[it] }.toMutableList().apply { add(toLinkIndex, removeAt(fromLinkIndex)) }
    val linkAtSlot = linkSlots.zip(links).toMap()
    return mapIndexed { index, component -> linkAtSlot[index] ?: component }
}
