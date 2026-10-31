package blue.deniz.vantage.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import blue.deniz.vantage.core.MarkdownBlock
import blue.deniz.vantage.core.MarkdownListItem
import blue.deniz.vantage.core.MarkdownSpan

private val QUOTE_BAR_WIDTH = 3.dp
private val LIST_MARKER_WIDTH = 24.dp
private val TABLE_CELL_MIN_WIDTH = 96.dp
private val CHECK_BOX_TOP_PADDING = 3.dp

@Composable
fun MarkdownView(
    blocks: List<MarkdownBlock>,
    onLinkClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        for (block in blocks) MarkdownBlockView(block, onLinkClick, color)
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, onLinkClick: (String) -> Unit, color: Color) {
    val typography = MaterialTheme.typography
    when (block) {
        is MarkdownBlock.Paragraph -> SpansText(block.spans, onLinkClick, typography.bodyLarge, color)
        is MarkdownBlock.Heading -> SpansText(
            block.spans,
            onLinkClick,
            when (block.level) {
                1 -> typography.titleLarge
                2 -> typography.titleMedium
                else -> typography.titleSmall
            },
            MaterialTheme.colorScheme.onSurface,
            Modifier.padding(top = Spacing.xs),
        )
        is MarkdownBlock.BulletList -> ListItems(block.items, onLinkClick, color) { _, item ->
            when (item.checked) {
                null -> ListMarkerText("•", color)
                else -> Icon(
                    if (item.checked == true) AppIcons.CheckBox else AppIcons.CheckBoxBlank,
                    Modifier.padding(top = CHECK_BOX_TOP_PADDING),
                    size = IconSize.sm,
                    tint = color,
                )
            }
        }
        is MarkdownBlock.NumberedList -> ListItems(block.items, onLinkClick, color) { index, _ -> ListMarkerText("${block.start + index}.", color) }
        is MarkdownBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(QUOTE_BAR_WIDTH).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
            MarkdownView(block.blocks, onLinkClick, Modifier.padding(start = Spacing.md), color)
        }
        is MarkdownBlock.Code -> Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.sm))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .horizontalScroll(rememberScrollState())
                .padding(Spacing.sm),
        ) {
            MaterialText(block.text, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, style = typography.bodyMedium)
        }
        is MarkdownBlock.Table -> TableView(block, onLinkClick, color)
        MarkdownBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = Spacing.xs))
    }
}

@Composable
private fun ListItems(
    items: List<MarkdownListItem>,
    onLinkClick: (String) -> Unit,
    color: Color,
    marker: @Composable (index: Int, item: MarkdownListItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        items.forEachIndexed { index, item ->
            Row {
                Box(Modifier.widthIn(min = LIST_MARKER_WIDTH)) { marker(index, item) }
                MarkdownView(item.blocks, onLinkClick, Modifier.weight(1f), color)
            }
        }
    }
}

@Composable
private fun ListMarkerText(text: String, color: Color) {
    MaterialText(text, color = color, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun TableView(table: MarkdownBlock.Table, onLinkClick: (String) -> Unit, color: Color) {
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        TableRow(table.header, onLinkClick, MaterialTheme.colorScheme.onSurface, header = true)
        HorizontalDivider()
        for (row in table.rows) TableRow(row, onLinkClick, color, header = false)
    }
}

@Composable
private fun TableRow(cells: List<List<MarkdownSpan>>, onLinkClick: (String) -> Unit, color: Color, header: Boolean) {
    val style = if (header) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
    Row(verticalAlignment = Alignment.Top) {
        for (cell in cells) {
            SpansText(cell, onLinkClick, style, color, Modifier.widthIn(min = TABLE_CELL_MIN_WIDTH).padding(vertical = Spacing.xs, horizontal = Spacing.sm))
        }
    }
}

@Composable
private fun SpansText(spans: List<MarkdownSpan>, onLinkClick: (String) -> Unit, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    MaterialText(annotatedSpans(spans, linkStyle, codeBackground, onLinkClick), modifier, color = color, style = style)
}

private fun annotatedSpans(
    spans: List<MarkdownSpan>,
    linkStyle: TextLinkStyles,
    codeBackground: Color,
    onLinkClick: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    for (span in spans) {
        val style = SpanStyle(
            fontWeight = FontWeight.Bold.takeIf { span.bold },
            fontStyle = FontStyle.Italic.takeIf { span.italic },
            fontFamily = FontFamily.Monospace.takeIf { span.code },
            background = if (span.code) codeBackground else Color.Unspecified,
            textDecoration = TextDecoration.LineThrough.takeIf { span.strikethrough },
        )
        val link = span.link
        if (link == null) {
            withStyle(style) { append(span.text) }
        } else {
            withLink(LinkAnnotation.Clickable(link, linkStyle) { onLinkClick(link) }) { withStyle(style) { append(span.text) } }
        }
    }
}
