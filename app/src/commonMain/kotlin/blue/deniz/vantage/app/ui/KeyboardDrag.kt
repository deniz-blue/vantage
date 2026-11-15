package blue.deniz.vantage.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

interface KeyboardDrag {
    val height: Int

    val isControllingKeyboard: Boolean

    fun lowerBy(px: Float): Float

    suspend fun settle(velocityY: Float)

    fun release()
}

@Composable
expect fun rememberKeyboardDrag(): KeyboardDrag?

val LocalKeyboardDrag = staticCompositionLocalOf<KeyboardDrag?> { null }
