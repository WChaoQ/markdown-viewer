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
}
