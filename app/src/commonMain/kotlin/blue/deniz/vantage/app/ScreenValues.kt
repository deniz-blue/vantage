package blue.deniz.vantage.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow

class ScreenValueStore {
    private val byScreen = mutableMapOf<String, MutableMap<String, Any?>>()

    fun valuesFor(screenKey: String): MutableMap<String, Any?> = byScreen.getOrPut(screenKey) { mutableMapOf() }

    fun forget(screenKey: String) {
        byScreen.remove(screenKey)
    }
}

val LocalScreenValues = staticCompositionLocalOf<MutableMap<String, Any?>> { mutableMapOf() }

val LocalStackDepth = staticCompositionLocalOf { 0 }

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> rememberAcrossNavigation(key: String, create: () -> T): T {
    val values = LocalScreenValues.current
    return remember(values, key) { values.getOrPut(key, create) as T }
}

private class LatestValue<T>(val input: Any?, val value: T)

@Composable
fun <T> collectAcrossNavigation(key: String, input: Any?, observe: () -> Flow<T>): T? {
    val latest = rememberAcrossNavigation(key) { mutableStateOf<LatestValue<T>?>(null) }
    LaunchedEffect(latest, input) { observe().collect { latest.value = LatestValue(input, it) } }
    return latest.value?.takeIf { it.input == input }?.value
}

val LocalSessionValues = staticCompositionLocalOf<MutableMap<String, MutableState<*>>> { mutableMapOf() }

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> collectAcrossSession(key: String, restartOn: Any?, observe: () -> Flow<T>): T? {
    val values = LocalSessionValues.current
    val latest = remember(values, key) { values.getOrPut(key) { mutableStateOf<T?>(null) } as MutableState<T?> }
    LaunchedEffect(latest, restartOn) { observe().collect { latest.value = it } }
    return latest.value
}

@Suppress("UNCHECKED_CAST")
@Composable
fun <T> latestAcrossSession(key: String): T? = (LocalSessionValues.current[key] as MutableState<T?>?)?.value
