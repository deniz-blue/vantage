package blue.deniz.vantage.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class ResultCacheTest {
    @Test
    fun aStoredValueIsFreshUntilItsTimeRunsOut() {
        val clock = SteppedClock()
        val cache = ResultCache<String, Int>(10.minutes, clock)
        cache.store("a", 1)
        clock.advance(9.minutes)
        assertEquals(1, cache.fresh("a"))
        clock.advance(1.minutes)
        assertNull(cache.fresh("a"))
        assertEquals(1, cache.last("a"))
    }

    @Test
    fun loadsOnlyWhenNothingFreshIsStored() = runTest {
        val clock = SteppedClock()
        val cache = ResultCache<String, Int>(10.minutes, clock)
        var loads = 0
        assertEquals(1, cache.freshOrLoad("a") { ++loads })
        assertEquals(1, cache.freshOrLoad("a") { ++loads })
        clock.advance(10.minutes)
        assertEquals(2, cache.freshOrLoad("a") { ++loads })
    }

    @Test
    fun aFailedLoadStoresNothing() = runTest {
        val cache = ResultCache<String, Int>(10.minutes, SteppedClock())
        runCatching { cache.freshOrLoad("a") { error("offline") } }
        assertNull(cache.last("a"))
    }
}
