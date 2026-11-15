package blue.deniz.vantage.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

@Composable
actual fun SystemBackHandler(enabled: Boolean, handlesEscape: Boolean, onBack: () -> Unit) = key(enabled) { BackHandler(enabled, onBack) }

actual fun handleEscapeKey(): Boolean = false
