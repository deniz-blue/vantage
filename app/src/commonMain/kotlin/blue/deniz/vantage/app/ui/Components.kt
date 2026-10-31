package blue.deniz.vantage.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.border
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.SnackbarHost as MaterialSnackbarHost
import androidx.compose.material3.SnackbarHostState

const val DISABLED_ALPHA = 0.38f
val IconButtonInset = (ControlHeight.md - IconSize.md) / 2
val ListRowGap = Spacing.xs
const val LOADING_SPINNER_DELAY_MILLIS = 300
private val LOADING_SPINNER_SIZE = 36.dp
val ListRowHeight = 44.dp
val ListRowPadding = Spacing.sm
val ListRowIconGap = ListRowPadding
private val LIST_ROW_HEIGHT_WITH_SUPPORTING = 60.dp

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    textAlign: TextAlign = TextAlign.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val withFlagImages = remember(text) { textWithFlagImages(text) }
    if (withFlagImages != null) {
        MaterialText(
            text = withFlagImages.text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            textAlign = textAlign,
            overflow = TextOverflow.Ellipsis,
            maxLines = maxLines,
            minLines = minLines,
            inlineContent = withFlagImages.inlineContent,
            style = style,
        )
        return
    }
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        textAlign = textAlign,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        minLines = minLines,
        style = style,
    )
}

@Composable
fun Icon(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = IconSize.md, tint: Color = LocalContentColor.current) {
    MaterialIcon(icon, contentDescription = null, modifier = modifier.size(size), tint = tint)
}

val CardShape = RoundedCornerShape(Radius.md)

@Composable
fun Card(
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.sm),
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickable = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(onLongClick = onLongClick, onClick = onClick ?: {})
    } else Modifier
    MaterialCard(modifier, shape = CardShape, colors = CardDefaults.cardColors(containerColor = background), border = border) {
        Column(clickable.padding(contentPadding), content = content)
    }
}

@Composable
fun IconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    busy: Boolean = false,
    busyProgress: Float? = null,
) {
    MaterialIconButton(onClick = onClick, modifier = modifier.size(ControlHeight.md).semantics { contentDescription = label }, enabled = enabled) {
        Crossfade(busy) { showsProgress ->
            if (showsProgress) {
                Spinner(IconSize.md, tint, progress = busyProgress)
            } else {
                Icon(icon, tint = if (enabled) tint else tint.copy(alpha = DISABLED_ALPHA))
            }
        }
    }
}

@Composable
fun Button(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    busy: Boolean = false,
    busyProgress: Float? = null,
) {
    val content: @Composable RowScope.() -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Row(Modifier.alpha(if (busy) 0f else 1f), verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) Icon(leadingIcon, Modifier.padding(end = Spacing.sm), size = ButtonDefaults.IconSize)
                Text(label, maxLines = 1)
                if (icon != null) Icon(icon, Modifier.padding(start = Spacing.sm), size = ButtonDefaults.IconSize)
            }
            if (busy) Spinner(ButtonDefaults.IconSize, LocalContentColor.current, progress = busyProgress)
        }
    }
    val onClickUnlessBusy = { if (!busy) onClick() }
    when {
        primary -> MaterialButton(onClickUnlessBusy, modifier, enabled = enabled, content = content)
        danger -> MaterialButton(
            onClickUnlessBusy,
            modifier,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            content = content,
        )
        else -> FilledTonalButton(onClickUnlessBusy, modifier, enabled = enabled, content = content)
    }
}

internal class ListSectionBounds {
    var coordinates: LayoutCoordinates? = null
}

internal val LocalListSection = staticCompositionLocalOf<ListSectionBounds?> { null }

@Composable
internal fun Modifier.dividerAboveInListSection(): Modifier {
    val section = LocalListSection.current ?: return this
    val color = MaterialTheme.colorScheme.outlineVariant
    var belowAnotherRow by remember { mutableStateOf(false) }
    return this
        .onPlaced { row -> section.coordinates?.takeIf { it.isAttached }?.let { belowAnotherRow = it.localPositionOf(row, Offset.Zero).y >= 1f } }
        .drawWithContent {
            drawContent()
            if (belowAnotherRow) drawLine(color, Offset.Zero, Offset(size.width, 0f), Dp.Hairline.toPx().coerceAtLeast(1f))
        }
}

@Composable
fun ListSection(modifier: Modifier = Modifier, title: String? = null, content: @Composable () -> Unit) {
    val bounds = remember { ListSectionBounds() }
    Column(modifier.padding(horizontal = Spacing.md)) {
        if (title != null) {
            SectionTitle(title, Modifier.padding(top = Spacing.sm, bottom = Spacing.xs))
        }
        Layout(
            content = { CompositionLocalProvider(LocalListSection provides bounds, content = content) },
            modifier = Modifier
                .onPlaced { bounds.coordinates = it }
                .clip(RoundedCornerShape(Radius.md))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            layout(constraints.maxWidth, placeables.sumOf { it.height }) {
                var top = 0
                for (placeable in placeables) {
                    placeable.place(0, top)
                    top += placeable.height
                }
            }
        }
    }
}

@Composable
fun ListRow(
    headline: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    alignWithIcons: Boolean = false,
    supporting: String? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    elevation: Dp = 0.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val inSection = LocalListSection.current != null
    val clickable = onClick != null || onLongClick != null
    val lifted = if (elevation > 0.dp) {
        Modifier
            .shadow(elevation, RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(Radius.md))
    } else Modifier
    val box = when {
        clickable && inSection -> lifted.combinedClickable(enabled = enabled, role = Role.Button, onLongClick = onLongClick, onClick = onClick ?: {})
        clickable -> Modifier
            .shadow(elevation, RoundedCornerShape(Radius.md))
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(enabled = enabled, role = Role.Button, onLongClick = onLongClick, onClick = onClick ?: {})
        else -> Modifier
    }
    val iconTint = if (color == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else color
    val leadingContent: (@Composable () -> Unit)? = when {
        leading != null -> ({ Box(Modifier.defaultMinSize(IconSize.md, IconSize.md), contentAlignment = Alignment.Center) { leading() } })
        icon != null -> ({ Icon(icon, tint = iconTint) })
        alignWithIcons -> ({ Spacer(Modifier.size(IconSize.md)) })
        else -> null
    }
    Row(
        modifier
            .then(if (inSection) Modifier else Modifier.padding(horizontal = Spacing.md, vertical = ListRowGap / 2))
            .dividerAboveInListSection()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(box)
            .semantics(mergeDescendants = true) {}
            .heightIn(min = if (supporting == null) ListRowHeight else LIST_ROW_HEIGHT_WITH_SUPPORTING)
            .padding(ListRowPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                Box(contentAlignment = Alignment.Center) { leadingContent() }
            }
            Spacer(Modifier.width(ListRowIconGap))
        }
        Column(Modifier.weight(1f)) {
            Text(headline, color = color, maxLines = 2, style = MaterialTheme.typography.bodyLarge)
            supporting?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (trailing != null) Spacer(Modifier.width(Spacing.sm))
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            trailing?.invoke()
        }
    }
}

@Composable
fun MenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    supportingText: String? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    busy: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    ListRow(
        label,
        modifier,
        icon = leadingIcon,
        supporting = supportingText,
        color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        enabled = enabled,
        onClick = { if (!busy) onClick() },
        onLongClick = onLongClick,
        leading = if (busy) ({ Spinner(IconSize.md, MaterialTheme.colorScheme.onSurfaceVariant) }) else null,
        trailing = trailingIcon?.let { { Icon(it) } },
    )
}

@Composable
fun Spinner(size: Dp, color: Color = MaterialTheme.colorScheme.primary, progress: Float? = null) {
    val modifier = Modifier.size(size).padding(size / 12)
    val stroke = size / 10
    if (progress == null) CircularProgressIndicator(modifier, color = color, strokeWidth = stroke)
    else CircularProgressIndicator(progress = { progress }, modifier, color = color, strokeWidth = stroke)
}

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier, progress: Float? = null) {
    val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
    Box(modifier.fillMaxWidth().padding(Spacing.md), contentAlignment = Alignment.Center) {
        AnimatedVisibility(shown, enter = fadeIn(tween(delayMillis = LOADING_SPINNER_DELAY_MILLIS))) {
            Spinner(LOADING_SPINNER_SIZE, progress = progress)
        }
    }
}

@Composable
fun CheckBoxIcon(checked: Boolean) {
    Icon(
        if (checked) AppIcons.CheckBox else AppIcons.CheckBoxBlank,
        tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun RemoveButton(label: String, onClick: () -> Unit) {
    IconButton(AppIcons.X, label, onClick, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun <Option> SegmentedControl(
    options: List<Pair<Option, String>>,
    selected: Option,
    onSelect: (Option) -> Unit,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val selectedIndex = options.indexOfFirst { it.first == selected }
    val thumbPosition by animateFloatAsState(selectedIndex.coerceAtLeast(0).toFloat(), SEGMENT_THUMB_MOTION)
    val track = if (floating) Modifier.floatingSurface(CircleShape).border(floatingBorder(), CircleShape) else Modifier.background(colors.surfaceContainerHigh, CircleShape)
    Row(
        modifier
            .fillMaxWidth()
            .height(ControlHeight.md)
            .then(track)
            .drawBehind {
                if (selectedIndex < 0) return@drawBehind
                val inset = SEGMENT_THUMB_INSET.toPx()
                val width = (size.width - inset * 2) / options.size
                val height = size.height - inset * 2
                drawRoundRect(
                    colors.primary.copy(alpha = SELECTED_TINT_ALPHA),
                    Offset(inset + width * thumbPosition, inset),
                    Size(width, height),
                    CornerRadius(height / 2),
                )
            }
            .padding(SEGMENT_THUMB_INSET)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, (option, label) ->
            val isSelected = index == selectedIndex
            val labelColor by animateColorAsState(if (isSelected) colors.primary else colors.onSurfaceVariant)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .selectable(isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(horizontal = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = labelColor, maxLines = 1, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

const val SELECTED_TINT_ALPHA = 0.18f
private val SEGMENT_THUMB_INSET = 3.dp
private val SEGMENT_THUMB_MOTION = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleSmall)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
}

@Composable
fun SheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, maxLines = 2, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun ListPlaceholder(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Spacing.sm),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun TitledSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.padding(horizontal = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionTitle(title)
        content()
    }
}

@Composable
fun SnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    MaterialSnackbarHost(state, modifier) { data ->
        val shape = RoundedCornerShape(Radius.md)
        Surface(
            modifier = Modifier.padding(Spacing.sm).widthIn(max = ContentMaxWidth).fillMaxWidth().floatingSurface(shape),
            shape = shape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = floatingBorder(),
        ) {
            Row(Modifier.heightIn(min = ControlHeight.lg).padding(start = Spacing.md, end = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(data.visuals.message, Modifier.weight(1f).padding(vertical = Spacing.sm), style = MaterialTheme.typography.bodyMedium)
                data.visuals.actionLabel?.let { label ->
                    TextButton(onClick = data::performAction) { Text(label, color = MaterialTheme.colorScheme.primary) }
                }
                if (data.visuals.withDismissAction) IconButton(AppIcons.X, "Dismiss", onClick = data::dismiss)
            }
        }
    }
}
