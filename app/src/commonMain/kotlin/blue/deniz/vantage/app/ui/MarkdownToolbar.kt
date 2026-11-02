package blue.deniz.vantage.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class MarkdownFormat(val label: String, val icon: ImageVector) {
    Bold("Bold", AppIcons.Bold),
    Italic("Italic", AppIcons.Italic),
    Strikethrough("Strikethrough", AppIcons.Strikethrough),
    Code("Code", AppIcons.Code),
    Link("Link", AppIcons.Link),
    Heading("Heading", AppIcons.Heading),
    Quote("Quote", AppIcons.Quote),
    BulletList("Bulleted List", AppIcons.BulletList),
}

fun TextFieldValue.withMarkdown(format: MarkdownFormat): TextFieldValue = when (format) {
    MarkdownFormat.Bold -> withMarksToggledAround("**")
    MarkdownFormat.Italic -> withMarksToggledAround("*")
    MarkdownFormat.Strikethrough -> withMarksToggledAround("~~")
    MarkdownFormat.Code -> withMarksToggledAround("`")
    MarkdownFormat.Link -> withSelectionLinked()
    MarkdownFormat.Heading -> withLinePrefixToggled("# ")
    MarkdownFormat.Quote -> withLinePrefixToggled("> ")
    MarkdownFormat.BulletList -> withLinePrefixToggled("- ")
}

private fun TextFieldValue.withMarksToggledAround(mark: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val marked = start >= mark.length && text.startsWith(mark, end) && text.substring(start - mark.length, start) == mark
    if (marked) {
        val unmarked = text.removeRange(end, end + mark.length).removeRange(start - mark.length, start)
        return TextFieldValue(unmarked, TextRange(start - mark.length, end - mark.length))
    }
    val inner = text.substring(start, end)
    return TextFieldValue(text.replaceRange(start, end, mark + inner + mark), TextRange(start + mark.length, end + mark.length))
}

private fun TextFieldValue.withSelectionLinked(): TextFieldValue {
    val start = selection.min
    val label = text.substring(start, selection.max)
    val link = "[$label]()"
    val cursor = if (label.isEmpty()) start + 1 else start + link.length - 1
    return TextFieldValue(text.replaceRange(start, selection.max, link), TextRange(cursor))
}

private fun TextFieldValue.withLinePrefixToggled(prefix: String): TextFieldValue {
    val firstLineStart = text.lastIndexOf('\n', selection.min - 1) + 1
    val lastLineEnd = text.indexOf('\n', selection.max).takeIf { it >= 0 } ?: text.length
    val lines = text.substring(firstLineStart, lastLineEnd).split('\n')
    val removing = lines.all { it.startsWith(prefix) }
    val changed = lines.map { line ->
        when {
            removing -> line.removePrefix(prefix)
            line.startsWith(prefix) -> line
            else -> prefix + line
        }
    }
    val joined = changed.joinToString("\n")
    val firstLineShift = changed.first().length - lines.first().length
    val totalShift = joined.length - (lastLineEnd - firstLineStart)
    return TextFieldValue(
        text.replaceRange(firstLineStart, lastLineEnd, joined),
        TextRange((selection.min + firstLineShift).coerceAtLeast(firstLineStart), selection.max + totalShift),
    )
}

@Composable
fun MarkdownToolbar(value: TextFieldValue, onChange: (TextFieldValue) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .glassBar(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xs)
            .focusProperties { canFocus = false },
    ) {
        MarkdownFormat.entries.forEach { format ->
            IconButton(format.icon, format.label, onClick = { onChange(value.withMarkdown(format)) })
        }
    }
}
