package blue.deniz.vantage.app.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
actual val WindowInsets.Companion.keyboardTarget: WindowInsets
    @Composable get() = imeAnimationTarget
