package com.wchaoq.markdownviewer

import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownHtmlRendererTest {
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
        val parsed = MarkdownHtmlRenderer.parse("# Reusable\\n\\n**Same content**")

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
    fun normalizesLineEndingsAndRemovesUtf8Bom() {
        val html = MarkdownHtmlRenderer.render("\uFEFF# First\r\n\r\nSecond\rThird", false)

        assertTrue(html.contains("<h1>First</h1>"))
        assertTrue(html.contains("<p>Second\nThird</p>"))
    }
}
