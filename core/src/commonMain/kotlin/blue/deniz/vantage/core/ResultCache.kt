package blue.deniz.vantage.core

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

@OptIn(ExperimentalAtomicApi::class)
class ResultCache<K, V>(private val freshFor: Duration, private val clock: Clock = Clock.System) {
    private class Entry<V>(val value: V, val storedAt: Instant)

    private val entries = AtomicReference(emptyMap<K, Entry<V>>())

    fun last(key: K): V? = entries.load()[key]?.value

    fun fresh(key: K): V? = entries.load()[key]?.takeIf { clock.now() - it.storedAt < freshFor }?.value

    fun store(key: K, value: V) {
        val entry = Entry(value, clock.now())
        while (true) {
            val current = entries.load()
            if (entries.compareAndSet(current, current + (key to entry))) return
        }
    }

    suspend fun freshOrLoad(key: K, load: suspend () -> V): V = fresh(key) ?: load().also { store(key, it) }
}
