package blue.deniz.vantage.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlurhashTest {
    @Test
    fun decodesLikeTheReferenceImplementation() {
        val expected = listOf(
            135, 164, 177, 161, 173, 177, 181, 180, 171, 160, 172, 174,
            124, 154, 169, 148, 148, 154, 164, 145, 134, 146, 152, 155,
            124, 144, 154, 144, 134, 132, 163, 130, 104, 148, 140, 134,
        )
        val pixels = assertNotNull(Blurhash.decodeToArgb("LEHV6nWB2yk8pyo0adR*.7kCMdnj", 4, 3))
        val actual = pixels.flatMap { listOf(it shr 16 and 255, it shr 8 and 255, it and 255) }
        assertEquals(expected.size, actual.size)
        assertTrue(expected.zip(actual).all { (e, a) -> abs(e - a) <= 1 }, "expected $expected, got $actual")
        assertTrue(pixels.all { it ushr 24 == 255 })
    }

    @Test
    fun rejectsMalformedHashes() {
        assertNull(Blurhash.decodeToArgb("", 4, 3))
        assertNull(Blurhash.decodeToArgb("LEHV6nWB2yk8", 4, 3))
        assertNull(Blurhash.decodeToArgb("LEHV6nWB2yk8pyo0adR*.7kCMdn\"", 4, 3))
    }
}
