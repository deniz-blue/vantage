package directory.evnt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AtRecordTypeTest {
    private fun eventTyped(type: String) =
        """{"${'$'}type":"$type","v":"0.2","id":"https://example.com/events/jam","name":{"en":"Game Jam"}}"""

    @Test
    fun anAtRecordOfAnOpenEvntParses() {
        assertEquals(mapOf("en" to "Game Jam"), OpenEvnt.parse(eventTyped(OpenEvnt.AT_RECORD_TYPE)).name)
    }

    @Test
    fun anotherRecordTypeIsRejected() {
        assertFailsWith<InvalidOpenEvntException> { OpenEvnt.parse(eventTyped("community.lexicon.calendar.event")) }
    }
}
