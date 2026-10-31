package blue.deniz.vantage.core

import org.intellij.markdown.IElementType
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.intellij.markdown.MarkdownElementTypes as Element
import org.intellij.markdown.MarkdownTokenTypes as Token
import org.intellij.markdown.flavours.gfm.GFMElementTypes as GfmElement
import org.intellij.markdown.flavours.gfm.GFMTokenTypes as GfmToken

data class MarkdownHighlight(val start: Int, val end: Int, val kind: Kind) {
    sealed interface Kind {
        data object Marker : Kind
        data object Bold : Kind
        data object Italic : Kind
        data object Strikethrough : Kind
        data object Code : Kind
        data object Link : Kind
        data object Quote : Kind
        data class Heading(val level: Int) : Kind
    }
}

fun Markdown.highlight(text: String): List<MarkdownHighlight> {
    val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(text)
    val highlights = buildList { MarkdownHighlighter(this).visit(root, parent = null) }
    val (markers, styles) = highlights.partition { it.kind == MarkdownHighlight.Kind.Marker }
    return styles + markers.sortedBy { it.start }.fold(emptyList()) { merged, next ->
        val last = merged.lastOrNull()
        if (last != null && last.end == next.start) merged.dropLast(1) + last.copy(end = next.end) else merged + next
    }
}

private val MARKER_TOKENS = setOf(
    Token.ATX_HEADER, Token.SETEXT_1, Token.SETEXT_2, Token.BLOCK_QUOTE, Token.LIST_BULLET, Token.LIST_NUMBER,
    GfmToken.CHECK_BOX, Token.HORIZONTAL_RULE, Token.CODE_FENCE_START, Token.CODE_FENCE_END, Token.FENCE_LANG,
)
private val MARKERS_BY_PARENT = mapOf(
    Element.EMPH to setOf(Token.EMPH),
    Element.STRONG to setOf(Token.EMPH),
    GfmElement.STRIKETHROUGH to setOf(GfmToken.TILDE),
    Element.CODE_SPAN to setOf(Token.BACKTICK),
    Element.IMAGE to setOf(Token.EXCLAMATION_MARK),
)
private val LINKS_WITH_TEXT = setOf(Element.INLINE_LINK, Element.FULL_REFERENCE_LINK)

private class MarkdownHighlighter(private val highlights: MutableList<MarkdownHighlight>) {
    fun visit(node: ASTNode, parent: ASTNode?) {
        val linkSyntax = node.type == Element.LINK_DEFINITION || (parent?.type in LINKS_WITH_TEXT && node.type != Element.LINK_TEXT)
        if (linkSyntax) return add(node, MarkdownHighlight.Kind.Marker)
        when {
            node.type in MARKER_TOKENS -> add(node, MarkdownHighlight.Kind.Marker)
            parent != null && node.type in MARKERS_BY_PARENT[parent.type].orEmpty() -> add(node, MarkdownHighlight.Kind.Marker)
            node.type == Element.LINK_TEXT || (node.type == Element.LINK_LABEL && parent?.type == Element.SHORT_REFERENCE_LINK) -> addLinkText(node)
        }
        styleOf(node.type)?.let { add(node, it) }
        node.children.forEach { visit(it, node) }
    }

    private fun styleOf(type: IElementType): MarkdownHighlight.Kind? = when (type) {
        Element.STRONG -> MarkdownHighlight.Kind.Bold
        Element.EMPH -> MarkdownHighlight.Kind.Italic
        GfmElement.STRIKETHROUGH -> MarkdownHighlight.Kind.Strikethrough
        Element.CODE_SPAN, Element.CODE_FENCE, Element.CODE_BLOCK -> MarkdownHighlight.Kind.Code
        Element.BLOCK_QUOTE -> MarkdownHighlight.Kind.Quote
        Element.AUTOLINK, GfmToken.GFM_AUTOLINK, Token.EMAIL_AUTOLINK -> MarkdownHighlight.Kind.Link
        in HEADING_LEVELS -> MarkdownHighlight.Kind.Heading(HEADING_LEVELS.getValue(type))
        else -> null
    }

    private fun addLinkText(text: ASTNode) {
        val open = text.children.firstOrNull() ?: return
        val close = text.children.last()
        add(open, MarkdownHighlight.Kind.Marker)
        if (close !== open) add(close, MarkdownHighlight.Kind.Marker)
        if (close.startOffset > open.endOffset) highlights += MarkdownHighlight(open.endOffset, close.startOffset, MarkdownHighlight.Kind.Link)
    }

    private fun add(node: ASTNode, kind: MarkdownHighlight.Kind) {
        if (node.endOffset > node.startOffset) highlights += MarkdownHighlight(node.startOffset, node.endOffset, kind)
    }
}
