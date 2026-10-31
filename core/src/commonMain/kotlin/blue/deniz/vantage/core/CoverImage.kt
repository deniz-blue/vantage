package blue.deniz.vantage.core

import directory.evnt.Component
import directory.evnt.Media
import directory.evnt.OpenEvnt

const val BACKGROUND_ROLE = "background"

private fun Component.isBackgroundSplash() = this is Component.SplashMedia && BACKGROUND_ROLE in roles.orEmpty()

fun OpenEvnt.backgroundSplashMedia(): Media? =
    components.orEmpty().firstOrNull { it.isBackgroundSplash() }?.let { (it as Component.SplashMedia).media }

fun OpenEvnt.withBackgroundSplashMedia(media: Media?): OpenEvnt {
    val current = components.orEmpty()
    val index = current.indexOfFirst { it.isBackgroundSplash() }
    if (index < 0) return if (media == null) this else copy(components = current + Component.SplashMedia(media, listOf(BACKGROUND_ROLE)))
    val existing = current[index] as Component.SplashMedia
    val otherRoles = existing.roles.orEmpty() - BACKGROUND_ROLE
    val replacement = when {
        media != null -> existing.copy(media = media)
        otherRoles.isNotEmpty() -> existing.copy(roles = otherRoles)
        else -> null
    }
    return copy(components = current.mapIndexedNotNull { i, component -> if (i == index) replacement else component })
}

fun Media?.withImageAddress(url: String): Media =
    if (this != null && sources.firstOrNull()?.url == url) this else Media(sources = listOf(Media.Source(url)), alt = this?.alt)
