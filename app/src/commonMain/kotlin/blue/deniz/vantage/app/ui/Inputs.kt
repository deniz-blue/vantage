package blue.deniz.vantage.app.ui

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

val InputShape = RoundedCornerShape(Radius.md)

private val InputOutlineWidth = 2.dp

@Composable
fun Modifier.inputOutline(focused: Boolean, isError: Boolean = false): Modifier {
    val color = when {
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> return this
    }
    return border(InputOutlineWidth, color, InputShape)
}

@Composable
fun FieldFrame(
    label: String?,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    field: @Composable () -> Unit,
) {
    if (label == null && supportingText == null) {
        Column(modifier) { field() }
        return
    }
    val noteColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        label?.let { Text(it, color = noteColor, maxLines = 1, style = MaterialTheme.typography.labelLarge) }
        field()
        supportingText?.let { Text(it, color = noteColor, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    problem: String? = null,
    showsProblem: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    allowsLineBreaks: Boolean = !singleLine,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    var leftWithProblem by rememberSaveable { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (wasFocused && !focused && problem != null) leftWithProblem = true
        wasFocused = focused
    }
    LaunchedEffect(value.isBlank()) { if (value.isBlank()) leftWithProblem = false }
    val shownProblem = problem?.takeIf { showsProblem || leftWithProblem }
    val showsError = isError || shownProblem != null
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        errorContainerColor = container,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
    )
    val textColor = textStyle.color.takeOrElse { MaterialTheme.colorScheme.onSurface }
    FieldFrame(label, modifier, shownProblem ?: supportingText, showsError) {
        BasicTextField(
            value = value,
            onValueChange = if (allowsLineBreaks) onValueChange else { text -> onValueChange(text.replace(LINE_BREAKS, " ")) },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (allowsLineBreaks || singleLine) Modifier else Modifier.onPreviewKeyEvent { event ->
                    val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                    if (enter && event.type == KeyEventType.KeyDown) keyboardActions.run(keyboardOptions.imeAction)
                    enter
                })
                .inputOutline(focused, showsError)
                .semantics { (label ?: placeholder)?.let { contentDescription = it } },
            textStyle = textStyle.copy(color = textColor),
            cursorBrush = SolidColor(if (showsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            interactionSource = interactions,
            decorationBox = { innerTextField ->
                Row(Modifier.background(container, InputShape).heightIn(min = ControlHeight.lg), verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        if (leading != null) Box(Modifier.padding(start = Spacing.sm)) { leading() }
                        Box(Modifier.weight(1f)) {
                            TextFieldDefaults.DecorationBox(
                                value = value,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = singleLine,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = interactions,
                                isError = showsError,
                                placeholder = placeholder?.let { { Text(it, style = textStyle, maxLines = 1) } },
                                shape = InputShape,
                                colors = colors,
                                contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xs),
                                container = {},
                            )
                        }
                        if (trailing != null) trailing()
                    }
                }
            },
        )
    }
}

private val LINE_BREAKS = Regex("[ \\t]*[\\r\\n]+[ \\t]*")

private object NoDefaultKeyboardAction : KeyboardActionScope {
    override fun defaultKeyboardAction(imeAction: ImeAction) = Unit
}

private fun KeyboardActions.run(imeAction: ImeAction) {
    val action = when (imeAction) {
        ImeAction.Done -> onDone
        ImeAction.Go -> onGo
        ImeAction.Next -> onNext
        ImeAction.Previous -> onPrevious
        ImeAction.Search -> onSearch
        ImeAction.Send -> onSend
        else -> null
    }
    action?.invoke(NoDefaultKeyboardAction)
}

private const val SHOWN_FOCUS_ATTEMPTS = 60

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onSubmit: (() -> Unit)? = null,
    floating: Boolean = true,
    focusesWhenShown: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    if (focusesWhenShown) LaunchedEffect(Unit) {
        // A composeunstyled drawer turns canFocus off for its content until it has settled open.
        repeat(SHOWN_FOCUS_ATTEMPTS) {
            withFrameNanos {}
            if (focused) return@LaunchedEffect
            focus.requestFocus()
        }
    }
    val focusable = if (focusesWhenShown) modifier.focusRequester(focus).onFocusChanged { focused = it.hasFocus } else modifier
    TextInput(
        value,
        onValueChange,
        if (floating) focusable.floatingSurface(InputShape).border(floatingBorder(), InputShape) else focusable,
        placeholder = placeholder,
        leading = leading ?: { Icon(AppIcons.Search) },
        trailing = trailing,
        keyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Search),
        keyboardActions = KeyboardActions(onAny = { onSubmit?.invoke() }),
        container = if (floating) Color.Transparent else colors.surfaceContainerHigh,
    )
}
