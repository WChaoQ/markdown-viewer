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
        assertTrue(html.contains("<del>Removed text</del>"))
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
