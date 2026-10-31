package blue.deniz.vantage.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import blue.deniz.vantage.core.Markdown
import blue.deniz.vantage.core.MarkdownHighlight
import blue.deniz.vantage.core.highlight

@Composable
fun rememberMarkdownHighlighting(): VisualTransformation {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    return remember(colors, typography) {
        MarkdownHighlighting { kind ->
            when (kind) {
                MarkdownHighlight.Kind.Marker -> SpanStyle(color = colors.outline)
                MarkdownHighlight.Kind.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
                MarkdownHighlight.Kind.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
                MarkdownHighlight.Kind.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                MarkdownHighlight.Kind.Code -> SpanStyle(fontFamily = FontFamily.Monospace, background = colors.surfaceContainerHighest)
                MarkdownHighlight.Kind.Link -> SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)
                MarkdownHighlight.Kind.Quote -> SpanStyle(color = colors.onSurfaceVariant)
                is MarkdownHighlight.Kind.Heading -> when (kind.level) {
                    1 -> typography.titleLarge
                    2 -> typography.titleMedium
                    else -> typography.titleSmall
                }.toSpanStyle()
            }
        }
    }
}

private class MarkdownHighlighting(private val styleOf: (MarkdownHighlight.Kind) -> SpanStyle) : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(
        buildAnnotatedString {
            append(text)
            for (highlight in Markdown.highlight(text.text)) addStyle(styleOf(highlight.kind), highlight.start, highlight.end)
        },
        OffsetMapping.Identity,
    )
}
