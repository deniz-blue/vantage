package blue.deniz.vantage.core

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.intellij.markdown.MarkdownElementTypes as Element
import org.intellij.markdown.MarkdownTokenTypes as Token
import org.intellij.markdown.flavours.gfm.GFMElementTypes as GfmElement
import org.intellij.markdown.flavours.gfm.GFMTokenTypes as GfmToken

data class MarkdownSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val strikethrough: Boolean = false,
    val link: String? = null,
) {
    fun hasSameStyle(other: MarkdownSpan) = copy(text = "") == other.copy(text = "")
}

data class MarkdownListItem(val blocks: List<MarkdownBlock>, val checked: Boolean? = null)

sealed interface MarkdownBlock {
    data class Paragraph(val spans: List<MarkdownSpan>) : MarkdownBlock
    data class Heading(val level: Int, val spans: List<MarkdownSpan>) : MarkdownBlock
    data class BulletList(val items: List<MarkdownListItem>) : MarkdownBlock
    data class NumberedList(val start: Int, val items: List<MarkdownListItem>) : MarkdownBlock
    data class Quote(val blocks: List<MarkdownBlock>) : MarkdownBlock
    data class Code(val text: String) : MarkdownBlock
    data class Table(val header: List<List<MarkdownSpan>>, val rows: List<List<List<MarkdownSpan>>>) : MarkdownBlock
    data object Rule : MarkdownBlock
}

object Markdown {
    fun parse(text: String): List<MarkdownBlock> {
        val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(text)
        return MarkdownTreeReader(text).blocksOf(root)
    }
}

internal val HEADING_LEVELS = mapOf(
    Element.ATX_1 to 1, Element.ATX_2 to 2, Element.ATX_3 to 3,
    Element.ATX_4 to 4, Element.ATX_5 to 5, Element.ATX_6 to 6,
    Element.SETEXT_1 to 1, Element.SETEXT_2 to 2,
)
private val EMPHASIS_MARKERS = setOf(Token.EMPH, GfmToken.TILDE)
private val ESCAPED_PUNCTUATION = Regex("""\\([!-/:-@\[-`{-~])""")
private val HTML_TAG = Regex("<[^>]*>")
private val NAMED_ENTITIES = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00A0")
private val ENTITY = Regex("&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z]+);")

private class MarkdownTreeReader(private val source: String) {
    private val definitions = mutableMapOf<String, String>()

    fun blocksOf(root: ASTNode): List<MarkdownBlock> {
        collectDefinitions(root)
        return blocks(root.children)
    }

    private fun collectDefinitions(node: ASTNode) {
        if (node.type == Element.LINK_DEFINITION) {
            val label = node.child(Element.LINK_LABEL)?.let(::labelKey)
            val destination = node.child(Element.LINK_DESTINATION)?.let(::destinationOf)
            if (label != null && destination != null) definitions.getOrPut(label) { destination }
        }
        node.children.forEach(::collectDefinitions)
    }

    private fun blocks(nodes: List<ASTNode>): List<MarkdownBlock> = nodes.mapNotNull(::block)

    private fun block(node: ASTNode): MarkdownBlock? = when (node.type) {
        Element.PARAGRAPH -> spans(node.children).trimmed().takeIf { it.isNotEmpty() }?.let(MarkdownBlock::Paragraph)
        in HEADING_LEVELS -> {
            val content = node.children.firstOrNull { it.type == Token.ATX_CONTENT || it.type == Token.SETEXT_CONTENT }
            MarkdownBlock.Heading(HEADING_LEVELS.getValue(node.type), spans(content?.children.orEmpty()).trimmed())
        }
        Element.UNORDERED_LIST -> MarkdownBlock.BulletList(listItems(node))
        Element.ORDERED_LIST -> {
            val number = node.child(Element.LIST_ITEM)?.child(Token.LIST_NUMBER)?.let(::textOf)
            MarkdownBlock.NumberedList(number?.takeWhile(Char::isDigit)?.toIntOrNull() ?: 1, listItems(node))
        }
        Element.BLOCK_QUOTE -> MarkdownBlock.Quote(blocks(node.children))
        Element.CODE_FENCE -> MarkdownBlock.Code(fencedCode(node))
        Element.CODE_BLOCK -> MarkdownBlock.Code(textOf(node).lines().joinToString("\n") { it.removeIndent() })
        GfmElement.TABLE -> table(node)
        Token.HORIZONTAL_RULE -> MarkdownBlock.Rule
        Element.HTML_BLOCK -> textOf(node).replace(HTML_TAG, "").trim().takeIf { it.isNotEmpty() }
            ?.let { MarkdownBlock.Paragraph(listOf(MarkdownSpan(unescaped(it)))) }
        else -> null
    }

    private fun listItems(list: ASTNode) = list.children.filter { it.type == Element.LIST_ITEM }.map { item ->
        val checkBox = item.child(GfmToken.CHECK_BOX)?.let(::textOf)
        MarkdownListItem(blocks(item.children), checked = checkBox?.let { 'x' in it.lowercase() })
    }

    private fun fencedCode(fence: ASTNode): String {
        val content = fence.children.filter { it.type == Token.CODE_FENCE_CONTENT }
        if (content.isEmpty()) return ""
        return source.substring(content.first().startOffset, content.last().endOffset)
    }

    private fun table(node: ASTNode): MarkdownBlock.Table {
        fun cells(row: ASTNode) = row.children.filter { it.type == GfmToken.CELL }.map { spans(it.children).trimmed() }
        val header = node.child(GfmElement.HEADER)?.let(::cells).orEmpty()
        return MarkdownBlock.Table(header, node.children.filter { it.type == GfmElement.ROW }.map(::cells))
    }

    private fun spans(nodes: List<ASTNode>, style: MarkdownSpan = MarkdownSpan("")): List<MarkdownSpan> =
        nodes.flatMap { inline(it, style) }.merged()

    private fun inline(node: ASTNode, style: MarkdownSpan): List<MarkdownSpan> = when (node.type) {
        Element.EMPH -> spans(node.children.withoutMarkers(), style.copy(italic = true))
        Element.STRONG -> spans(node.children.withoutMarkers(), style.copy(bold = true))
        GfmElement.STRIKETHROUGH -> spans(node.children.withoutMarkers(), style.copy(strikethrough = true))
        Element.CODE_SPAN -> listOf(style.copy(text = codeSpanText(node), code = true))
        Element.INLINE_LINK -> linked(node, node.child(Element.LINK_DESTINATION)?.let(::destinationOf), style)
        Element.FULL_REFERENCE_LINK, Element.SHORT_REFERENCE_LINK -> {
            val destination = node.child(Element.LINK_LABEL)?.let { definitions[labelKey(it)] }
            if (destination == null) listOf(style.copy(text = textOf(node))) else linked(node, destination, style)
        }
        Element.IMAGE -> node.child(Element.INLINE_LINK)?.let { link ->
            val destination = link.child(Element.LINK_DESTINATION)?.let(::destinationOf)
            linked(link, destination, style).ifEmpty { listOf(style.copy(text = "Image", link = destination)) }
        }.orEmpty()
        Element.AUTOLINK -> node.child(Token.AUTOLINK)?.let(::textOf)?.let { listOf(style.copy(text = it, link = it)) }.orEmpty()
        GfmToken.GFM_AUTOLINK -> textOf(node).let { listOf(style.copy(text = it, link = if ("://" in it) it else "https://$it")) }
        Token.EMAIL_AUTOLINK -> textOf(node).removeSurrounding("<", ">").let { listOf(style.copy(text = it, link = "mailto:$it")) }
        Token.EOL, Token.WHITE_SPACE -> listOf(style.copy(text = " "))
        Token.HARD_LINE_BREAK -> listOf(style.copy(text = "\n"))
        Token.ESCAPED_BACKTICKS -> listOf(style.copy(text = textOf(node).replace("\\", "")))
        Token.HTML_TAG, GfmToken.CHECK_BOX -> emptyList()
        Token.TEXT -> listOf(style.copy(text = unescaped(textOf(node))))
        else -> if (node.children.isEmpty()) listOf(style.copy(text = textOf(node))) else node.children.flatMap { inline(it, style) }
    }

    private fun linked(link: ASTNode, destination: String?, style: MarkdownSpan): List<MarkdownSpan> {
        val text = (link.child(Element.LINK_TEXT) ?: link.child(Element.LINK_LABEL))?.children?.drop(1)?.dropLast(1).orEmpty()
        val shown = spans(text, style.copy(link = destination)).trimmed()
        return shown.ifEmpty { destination?.let { listOf(style.copy(text = it, link = it)) }.orEmpty() }
    }

    private fun codeSpanText(node: ASTNode): String {
        val inner = node.children.dropWhile { it.type == Token.BACKTICK }.dropLastWhile { it.type == Token.BACKTICK }
        if (inner.isEmpty()) return ""
        return source.substring(inner.first().startOffset, inner.last().endOffset).replace('\n', ' ').trim()
    }

    private fun destinationOf(node: ASTNode) = unescaped(textOf(node).removeSurrounding("<", ">"))

    private fun labelKey(label: ASTNode) = textOf(label).removeSurrounding("[", "]").trim().lowercase().replace(Regex("\\s+"), " ")

    private fun textOf(node: ASTNode) = source.substring(node.startOffset, node.endOffset)
}

private fun ASTNode.child(type: org.intellij.markdown.IElementType) = children.firstOrNull { it.type == type }

private fun List<ASTNode>.withoutMarkers() = dropWhile { it.type in EMPHASIS_MARKERS }.dropLastWhile { it.type in EMPHASIS_MARKERS }

private fun String.removeIndent(): String = removePrefix(take(4).takeWhile { it == ' ' }).removePrefix("\t")

private fun List<MarkdownSpan>.merged(): List<MarkdownSpan> = fold(emptyList()) { merged, next ->
    val last = merged.lastOrNull()
    val afterSpace = last != null && !next.code && (last.text.endsWith(' ') || last.text.endsWith('\n'))
    val span = if (afterSpace) next.copy(text = next.text.trimStart(' ')) else next
    when {
        span.text.isEmpty() -> merged
        last != null && last.hasSameStyle(span) -> merged.dropLast(1) + last.copy(text = last.text + span.text)
        else -> merged + span
    }
}

private fun List<MarkdownSpan>.trimmed(): List<MarkdownSpan> {
    if (isEmpty()) return this
    val edited = mapIndexed { index, span ->
        val start = if (index == 0) span.text.trimStart() else span.text
        span.copy(text = if (index == lastIndex) start.trimEnd() else start)
    }
    return edited.filter { it.text.isNotEmpty() }
}

private fun unescaped(text: String): String = text
    .replace(ESCAPED_PUNCTUATION) { it.groupValues[1] }
    .replace(ENTITY) { match ->
        val name = match.groupValues[1]
        when {
            name.startsWith("#x") || name.startsWith("#X") -> name.drop(2).toIntOrNull(16)?.let(::codePointText)
            name.startsWith("#") -> name.drop(1).toIntOrNull()?.let(::codePointText)
            else -> NAMED_ENTITIES[name]
        } ?: match.value
    }
