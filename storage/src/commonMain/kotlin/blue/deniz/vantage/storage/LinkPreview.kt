package blue.deniz.vantage.storage

import blue.deniz.vantage.core.ResolvedEvent

sealed interface LinkPreview {
    data class New(val event: ResolvedEvent) : LinkPreview

    data class AlreadyStored(val id: String, val event: ResolvedEvent) : LinkPreview

    data class Broken(val event: ResolvedEvent, val message: String) : LinkPreview

    data class NotAnEvent(val message: String) : LinkPreview
}
