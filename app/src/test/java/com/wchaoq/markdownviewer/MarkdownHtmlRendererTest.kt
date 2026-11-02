package com.wchaoq.markdownviewer

import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownHtmlRendererTest {
    @Test
    fun assemblesHtmlAroundParsedBodyWithoutChangingContent() {
        val html = MarkdownHtmlRenderer.render("# Body boundary", false)

        assertTrue(html.contains("<body><h1>Body boundary</h1></body>"))
        assertTrue(html.endsWith("</html>"))
    }

    @Test
    fun rendersBasicMarkdown() {
        val html = MarkdownHtmlRenderer.render("# Hello\n\nA **bold** paragraph.", false)
        assertTrue(html.contains("<h1>Hello</h1>"))
        assertTrue(html.contains("<strong>bold</strong>"))
    }

    @Test
    fun rendersGfmTable() {
        val html = MarkdownHtmlRenderer.render(
            "| Name | Value |\n| --- | --- |\n| Kotlin | Android |",
            false
        )
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("Kotlin"))
    }

    @Test
    fun rendersGfmTaskListsAndStrikethrough() {
        val html = MarkdownHtmlRenderer.render(
            "- [x] Completed\n- [ ] Pending\n\n~~Removed text~~",
            false
        )

        assertTrue(html.contains("type=\"checkbox\""))
        assertTrue(html.contains("<span class=\"user-del\">Removed text</span>"))
        assertTrue(html.contains(".user-del { text-decoration: line-through; }"))
    }

    @Test
    fun rendersFencedCodeBlocksAndAutolinks() {
        val html = MarkdownHtmlRenderer.render(
            "```kotlin\nval answer = 42\n```\n\nhttps://example.com",
            false
        )

        assertTrue(html.contains("<pre>"))
        assertTrue(html.contains("val answer = 42"))
        assertTrue(html.contains("href=\"https://example.com\""))
    }


    @Test
    fun appliesRestrictiveContentSecurityPolicy() {
        val html = MarkdownHtmlRenderer.render("# Local document", false)

        assertTrue(html.contains("Content-Security-Policy"))
        assertTrue(html.contains("default-src 'none'"))
        assertTrue(html.contains("img-src data:"))
        assertTrue(html.contains("object-src 'none'"))
        assertTrue(html.contains("frame-src 'none'"))
        assertTrue(html.contains("form-action 'none'"))
        assertTrue(html.contains("name=\"referrer\" content=\"no-referrer\""))
    }

    @Test
    fun appliesResponsiveStylesForWideContent() {
        val html = MarkdownHtmlRenderer.render(
            "| A very wide heading | Another heading |\n| --- | --- |\n| a long value | another long value |\n\n```\nA very long code line that should remain horizontally scrollable.\n```",
            false
        )

        assertTrue(html.contains("overflow-x: auto"))
        assertTrue(html.contains("max-width: 100%"))
        assertTrue(html.contains("white-space: pre"))
        assertTrue(html.contains("viewport"))
    }

    @Test
    fun reusesParsedDocumentForLightAndDarkRendering() {
        val parsed = MarkdownHtmlRenderer.parse("# Reusable\n\n**Same content**")

        val lightHtml = MarkdownHtmlRenderer.render(parsed, false)
        val darkHtml = MarkdownHtmlRenderer.render(parsed, true)

        assertTrue(lightHtml.contains("<h1>Reusable</h1>"))
        assertTrue(darkHtml.contains("<h1>Reusable</h1>"))
        assertTrue(lightHtml.contains("color-scheme: light"))
        assertTrue(darkHtml.contains("color-scheme: dark"))
    }

    @Test
    fun usesMatchingLightAndDarkColorSchemes() {
        val lightHtml = MarkdownHtmlRenderer.render("# Theme", false)
        val darkHtml = MarkdownHtmlRenderer.render("# Theme", true)

        assertTrue(lightHtml.contains("color-scheme: light"))
        assertTrue(lightHtml.contains("#FFFBFE"))
        assertTrue(darkHtml.contains("color-scheme: dark"))
        assertTrue(darkHtml.contains("#121212"))
    }

    @Test
    fun preservesTextThatLooksLikeTemplatePlaceholders() {
        val markdown = "BG FG LINK CODE SURFACE BORDER COLOR_SCHEME HTML_BODY"
        val html = MarkdownHtmlRenderer.render(markdown, false)

        assertTrue(html.contains(markdown))
    }

    @Test
    fun removesOnlyLeadingBomAndPreservesInternalBom() {
        val markdown = "\uFEFF# First\n\nSecond\uFEFF text"
        val html = MarkdownHtmlRenderer.render(markdown, false)

        assertTrue(html.contains("<h1>First</h1>"))
        assertTrue(html.contains("Second\uFEFF text"))
    }

    @Test
    fun leavesUnchangedMarkdownContentIntact() {
        val markdown = "# Plain\n\nNo carriage returns or byte-order mark."
        val parsed = MarkdownHtmlRenderer.parse(markdown)
        val html = MarkdownHtmlRenderer.render(parsed, false)

        assertTrue(html.contains("<h1>Plain</h1>"))
        assertTrue(html.contains("No carriage returns or byte-order mark."))
    }

    @Test
    fun normalizesLineEndingsAndRemovesUtf8Bom() {
        val html = MarkdownHtmlRenderer.render("\uFEFF# First\r\n\r\nSecond\rThird", false)

        assertTrue(html.contains("<h1>First</h1>"))
        assertTrue(html.contains("<p>Second\nThird</p>"))
    }
    @Test
    fun includesLocalMathAndMermaidRenderers() {
        val markdown = "Inline: ${'$'}x^2${'$'}\n\n```mermaid\nflowchart TD\n A --> B\n```"
        val html = MarkdownHtmlRenderer.render(markdown, false)
        assertTrue(html.contains("/assets/markdown/katex/katex.min.js"))
        assertTrue(html.contains("/assets/markdown/katex/auto-render.min.js"))
        assertTrue(html.contains("/assets/markdown/mermaid.min.js"))
        assertTrue(html.contains("script-src 'self'"))
        assertTrue(html.contains("language-mermaid"))
    }
    @Test
    fun sanitizesExecutableRawHtmlAndRemoteResources() {
        val html = MarkdownHtmlRenderer.render(
            """<script>alert(1)</script><img src="https://example.com/image.png" onerror="alert(1)">""",
            false
        )
        assertTrue(!html.contains("alert(1)"))
        assertTrue(!html.contains("https://example.com/image.png"))
        assertTrue(html.contains("<img"))
    }


    @Test
    fun preservesLatexDelimitersBeforeMarkdownEscaping() {
        val markdown = """Inline \(x + y\) and display:

\[
x^2 + y^2 = z^2
\]"""

        val html = MarkdownHtmlRenderer.render(markdown, false)

        assertTrue(html.contains("""\(x + y\)"""))
        assertTrue(html.contains("""\[x^2 + y^2 = z^2\]"""))
        assertTrue(html.contains("/assets/markdown/katex/auto-render.min.js"))
    }

    @Test
    fun rendersGfmAlignmentAndEscapedTablePipes() {
        val html = MarkdownHtmlRenderer.render(
            "| Left | Center | Right |\n|:---|:---:|---:|\n| a\\|b | middle | end |",
            false
        )
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("Left"))
        assertTrue(html.contains("Center"))
        assertTrue(html.contains("Right"))
        assertTrue(html.contains("a|b"))
        assertTrue(html.contains("""th[align="center"]"""))
        assertTrue(html.contains("""td[align="right"]"""))
    }

    @Test
    fun rendersNestedListsBlockquotesAndHorizontalRules() {
        val html = MarkdownHtmlRenderer.render(
            "> A quote\n>\n> - first item\n>   - nested item\n\n---\n\n1. ordered\n   1. nested",
            false
        )
        assertTrue(html.contains("<blockquote>"))
        assertTrue(html.contains("nested item"))
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<hr"))
    }

    @Test
    fun doesNotLoadMermaidForOrdinaryMarkdown() {
        val html = MarkdownHtmlRenderer.render("# Plain document\n\nNo diagram here.", false)
        assertTrue(!html.contains("/assets/markdown/mermaid.min.js"))
        assertTrue(html.contains("/assets/markdown/katex/katex.min.js"))
    }

    @Test
    fun loadsMermaidOnlyWhenTheDocumentContainsADiagram() {
        val fence = 96.toChar().toString().repeat(3)
        val markdown = "## Diagram\n\n" + fence + "mermaid\nflowchart TD\n A --> B\n" + fence
        val html = MarkdownHtmlRenderer.render(markdown, false)
        assertTrue(html.contains("/assets/markdown/mermaid.min.js"))
        assertTrue(html.contains("language-mermaid"))
    }

    @Test
    fun emitsValidMultilineStylesForMathAndDiagrams() {
        val html = MarkdownHtmlRenderer.render("# CSS", false)
        assertTrue(html.contains(".mermaid-diagram {"))
        assertTrue(html.contains(".katex-display {"))
        assertTrue(!html.contains("vertical-align: middle; }\\n                .mermaid-diagram"))
    }

    @Test
    fun removesUnsafeLinkSchemesAndInlineEventHandlers() {
        val html = MarkdownHtmlRenderer.render(
            """[unsafe](javascript:alert(1)) <a href="data:text/html,bad" onclick="alert(2)">link</a>""",
            false
        )

        assertTrue(!html.contains("javascript:"))
        assertTrue(!html.contains("data:text/html"))
        assertTrue(!html.contains("onclick"))
        assertTrue(html.contains(">link</a>"))
    }

}
