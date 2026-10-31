package blue.deniz.vantage.core

import directory.evnt.Component
import directory.evnt.Media
import directory.evnt.OpenEvnt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoverImageTest {
    private val poster = Media(listOf(Media.Source("https://a.example/poster.jpg")))
    private val banner = Media(listOf(Media.Source("https://a.example/banner.jpg")))
    private val link = Component.Link("https://a.example")
    private fun event(vararg components: Component) = OpenEvnt(id = "e", name = emptyMap(), components = components.toList())

    @Test
    fun onlyMediaWithTheBackgroundRoleIsTheCover() {
        assertNull(event(Component.SplashMedia(poster, listOf("poster"))).backgroundSplashMedia())
        assertEquals(banner, event(Component.SplashMedia(poster), Component.SplashMedia(banner, listOf("background"))).backgroundSplashMedia())
    }

    @Test
    fun settingACoverAddsBackgroundSplashMedia() {
        assertEquals(event(link, Component.SplashMedia(banner, listOf("background"))), event(link).withBackgroundSplashMedia(banner))
    }

    @Test
    fun settingACoverReplacesTheMediaAndKeepsTheRoles() {
        val before = event(Component.SplashMedia(poster, listOf("poster", "background")), link)
        assertEquals(event(Component.SplashMedia(banner, listOf("poster", "background")), link), before.withBackgroundSplashMedia(banner))
    }

    @Test
    fun removingTheCoverKeepsMediaThatHasOtherRoles() {
        assertEquals(event(Component.SplashMedia(poster, listOf("poster")), link), event(Component.SplashMedia(poster, listOf("poster", "background")), link).withBackgroundSplashMedia(null))
        assertEquals(event(link), event(Component.SplashMedia(poster, listOf("background")), link).withBackgroundSplashMedia(null))
        assertEquals(event(link), event(link).withBackgroundSplashMedia(null))
    }

    @Test
    fun aNewAddressDropsTheOldImagesBlurhashButKeepsTheAltText() {
        val described = Media(listOf(Media.Source("https://a.example/old.jpg")), alt = mapOf("en" to "Hall"), presentation = Media.Presentation(blurhash = "LEHV6nWB2yk8"))
        assertEquals(described, described.withImageAddress("https://a.example/old.jpg"))
        assertEquals(Media(listOf(Media.Source("https://a.example/new.jpg")), alt = mapOf("en" to "Hall")), described.withImageAddress("https://a.example/new.jpg"))
        assertEquals(Media(listOf(Media.Source("https://a.example/new.jpg"))), null.withImageAddress("https://a.example/new.jpg"))
    }
}
