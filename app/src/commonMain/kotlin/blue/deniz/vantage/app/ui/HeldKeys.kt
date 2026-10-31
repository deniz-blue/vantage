package blue.deniz.vantage.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput

class HeldKeys {
    private var atLastPress: PointerKeyboardModifiers? = null

    val shiftAtLastPress get() = atLastPress?.isShiftPressed == true
    val ctrlOrMetaAtLastPress get() = atLastPress?.let { it.isCtrlPressed || it.isMetaPressed } == true

    val recorder: Modifier = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) atLastPress = event.keyboardModifiers
            }
        }
    }
}

@Composable
fun rememberHeldKeys(): HeldKeys = remember { HeldKeys() }
