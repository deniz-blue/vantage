package blue.deniz.vantage.core

import blue.deniz.vantage.core.MarkdownBlock.BulletList
import blue.deniz.vantage.core.MarkdownBlock.Code
import blue.deniz.vantage.core.MarkdownBlock.Heading
import blue.deniz.vantage.core.MarkdownBlock.NumberedList
import blue.deniz.vantage.core.MarkdownBlock.Paragraph
import blue.deniz.vantage.core.MarkdownBlock.Quote
import blue.deniz.vantage.core.MarkdownBlock.Table
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTest {
    private fun plain(text: String) = MarkdownSpan(text)

    @Test
    fun paragraphsJoinSoftLineBreaksAndKeepHardOnes() {
        assertEquals(
            listOf(Paragraph(listOf(plain("one two\nthree"))), Paragraph(listOf(plain("four")))),
            Markdown.parse("one\ntwo  \nthree\n\nfour"),
        )
    }

    @Test
    fun emphasisStrongCodeAndStrikethrough() {
        assertEquals(
            listOf(
                Paragraph(
                    listOf(
                        MarkdownSpan("bold", bold = true),
                        plain(", "),
                        MarkdownSpan("it", italic = true),
                        plain(", "),
                        MarkdownSpan("x = 1", code = true),
                        plain(", "),
                        MarkdownSpan("gone", strikethrough = true),
                        plain(", "),
                        MarkdownSpan("both", bold = true, italic = true),
                    ),
                ),
            ),
            Markdown.parse("**bold**, *it*, `x = 1`, ~~gone~~, ***both***"),
        )
    }

    @Test
    fun aLoneAsteriskStaysText() {
        assertEquals(listOf(Paragraph(listOf(plain("2 * 3 = 6")))), Markdown.parse("2 * 3 = 6"))
    }

    @Test
    fun linksOfEveryKindCarryTheirAddress() {
        val spans = (Markdown.parse(
            "[Tickets](https://a.example/t \"Buy\") <https://b.example> www.c.example https://d.example/e. [ref][r] [R] [missing]\n\n[r]: https://ref.example",
        ).single() as Paragraph).spans
        assertEquals(
            listOf(
                MarkdownSpan("Tickets", link = "https://a.example/t"),
                plain(" "),
                MarkdownSpan("https://b.example", link = "https://b.example"),
                plain(" "),
                MarkdownSpan("www.c.example", link = "https://www.c.example"),
                plain(" "),
                MarkdownSpan("https://d.example/e", link = "https://d.example/e"),
                plain(". "),
                MarkdownSpan("ref", link = "https://ref.example"),
                plain(" "),
                MarkdownSpan("R", link = "https://ref.example"),
                plain(" [missing]"),
            ),
            spans,
        )
    }

    @Test
    fun styledTextInsideALinkKeepsTheLink() {
        assertEquals(
            listOf(Paragraph(listOf(MarkdownSpan("Buy ", link = "https://a.example"), MarkdownSpan("now", bold = true, link = "https://a.example")))),
            Markdown.parse("[Buy **now**](https://a.example)"),
        )
    }

    @Test
    fun escapesAndEntitiesAreDecoded() {
        assertEquals(listOf(Paragraph(listOf(plain("*not bold* & <tag> © 😀")))), Markdown.parse("\\*not bold\\* &amp; &lt;tag&gt; &#169; &#x1F600;"))
    }

    @Test
    fun headingsOfBothStyles() {
        assertEquals(
            listOf(Heading(1, listOf(plain("Big"))), Heading(3, listOf(plain("Small"))), Heading(2, listOf(plain("Under")))),
            Markdown.parse("# Big\n### Small ###\n\nUnder\n---"),
        )
    }

    @Test
    fun listsNestAndNumberedListsKeepTheirStart() {
        assertEquals(
            listOf(
                NumberedList(
                    3,
                    listOf(
                        MarkdownListItem(listOf(Paragraph(listOf(plain("three"))))),
                        MarkdownListItem(
                            listOf(Paragraph(listOf(plain("four"))), BulletList(listOf(MarkdownListItem(listOf(Paragraph(listOf(plain("inner")))))))),
                        ),
                    ),
                ),
            ),
            Markdown.parse("3. three\n4. four\n   - inner"),
        )
    }

    @Test
    fun taskListItemsKnowWhetherTheyAreChecked() {
        assertEquals(
            listOf(
                BulletList(
                    listOf(
                        MarkdownListItem(listOf(Paragraph(listOf(plain("done")))), checked = true),
                        MarkdownListItem(listOf(Paragraph(listOf(plain("todo")))), checked = false),
                    ),
                ),
            ),
            Markdown.parse("- [x] done\n- [ ] todo"),
        )
    }

    @Test
    fun quotesCodeAndRules() {
        assertEquals(
            listOf(Quote(listOf(Paragraph(listOf(plain("said"))))), Code("val x = 1\nval y = 2"), MarkdownBlock.Rule, Code("indented")),
            Markdown.parse("> said\n\n```kotlin\nval x = 1\nval y = 2\n```\n\n***\n\n    indented"),
        )
    }

    @Test
    fun tablesSplitIntoCells() {
        assertEquals(
            listOf(Table(listOf(listOf(plain("Day")), listOf(plain("Hall"))), listOf(listOf(listOf(MarkdownSpan("Sat", bold = true)), listOf(plain("B")))))),
            Markdown.parse("| Day | Hall |\n|---|---|\n| **Sat** | B |"),
        )
    }

    @Test
    fun htmlTagsAreDropped() {
        assertEquals(listOf(Paragraph(listOf(plain("a b")))), Markdown.parse("a <br> b"))
        assertEquals(listOf(Paragraph(listOf(plain("block")))), Markdown.parse("<div>block</div>"))
    }
}
