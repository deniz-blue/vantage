package blue.deniz.vantage.app.ui

import androidx.compose.runtime.Composable

interface KeyboardDrag {
    val height: Int

    fun lowerBy(px: Float): Float

    suspend fun settle(velocityY: Float)
}

@Composable
expect fun rememberKeyboardDrag(): KeyboardDrag?
