package blue.deniz.vantage.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable

actual val WindowInsets.Companion.keyboardTarget: WindowInsets
    @Composable get() = ime
