package blue.deniz.vantage.ui

import androidx.compose.runtime.Composable

@Composable
expect fun SystemBackHandler(enabled: Boolean, handlesEscape: Boolean = false, onBack: () -> Unit)

expect fun handleEscapeKey(): Boolean
