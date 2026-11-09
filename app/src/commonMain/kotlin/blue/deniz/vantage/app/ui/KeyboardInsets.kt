package blue.deniz.vantage.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.flow.drop

expect val WindowInsets.Companion.keyboardTarget: WindowInsets
    @Composable get

@Composable
fun ClearFocusWhenKeyboardCloses() {
    val keyboard = WindowInsets.ime
    val keyboardTarget = WindowInsets.keyboardTarget
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(keyboard, keyboardTarget, density, focusManager) {
        snapshotFlow { keyboard.getBottom(density) > 0 || keyboardTarget.getBottom(density) > 0 }
            .drop(1)
            .collect { open -> if (!open) focusManager.clearFocus() }
    }
}
