package com.wchaoq.markdownviewer

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup
import java.util.Locale

internal class ParsedMarkdown internal constructor(val bodyHtml: String)

internal object MarkdownHtmlRenderer {
    private val unsafeAttributes = setOf(
        "style", "srcset", "srcdoc", "formaction", "action",
        "poster", "background", "xlink:href"
    )

    private fun normalizeLineEndings(markdown: String): String {
        val start = if (markdown.startsWith("\uFEFF")) 1 else 0
        val firstCarriageReturn = markdown.indexOf('\r', start)
        if (start == 0 && firstCarriageReturn < 0) return markdown

        val normalized = StringBuilder(markdown.length - start)
        var index = start
        while (index < markdown.length) {
            val character = markdown[index]
            if (character == '\r') {
                normalized.append('\n')
                if (index + 1 < markdown.length && markdown[index + 1] == '\n') {
                    index++
                }
            } else {
                normalized.append(character)
            }
            index++
        }
        return normalized.toString()
    }

    fun parse(markdown: String): ParsedMarkdown {
        val source = normalizeLineEndings(markdown)

        val flavour = GFMFlavourDescriptor()
        val tree = MarkdownParser(flavour).buildMarkdownTreeFromString(source)
        return ParsedMarkdown(sanitizeBody(HtmlGenerator(source, tree, flavour).generateHtml()))
    }

    fun render(markdown: String, darkTheme: Boolean): String =
        render(parse(markdown), darkTheme)

    fun render(parsed: ParsedMarkdown, darkTheme: Boolean): String {
        val body = parsed.bodyHtml

        val colors = if (darkTheme) {
            CssColors("#121212", "#E6E1E5", "#CAC4D0", "#211F26", "#49454F", "#D0BCFF", "#F2B8B5")
        } else {
            CssColors("#FFFBFE", "#1C1B1F", "#49454F", "#F3EDF7", "#CAC4D0", "#6750A4", "#8C1D18")
        }
        val colorScheme = if (darkTheme) "dark" else "light"
        val mermaidScript = if (body.contains("language-mermaid") || body.contains("lang-mermaid")) {
            """<script defer src="/assets/markdown/mermaid.min.js"></script>"""
        } else {
            ""
        }

        return buildString(body.length + 4096) {
            append(
                """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=5">
              <meta http-equiv="Content-Security-Policy" content="default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self'; img-src data:; connect-src 'none'; media-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'">
              <meta name="referrer" content="no-referrer">
              <link rel="stylesheet" href="/assets/markdown/katex/katex.min.css">
              <script defer src="/assets/markdown/katex/katex.min.js"></script>
              <script defer src="/assets/markdown/katex/auto-render.min.js"></script>
              $mermaidScript
              <script defer src="/assets/markdown/renderer.js"></script>
              <style>
                :root { color-scheme: $colorScheme; }
                * { box-sizing: border-box; }
                html { background: ${colors.background}; }
                body {
                  margin: 0; padding: 22px 20px 40px; color: ${colors.foreground}; background: ${colors.background};
                  font-family: -apple-system, BlinkMacSystemFont, "Roboto", "Noto Sans", sans-serif;
                  font-size: 16px; line-height: 1.72; overflow-wrap: anywhere;
                  -webkit-text-size-adjust: 100%;
                }
                h1, h2, h3, h4, h5, h6 { color: ${colors.foreground}; line-height: 1.3; font-weight: 650; margin: 1.55em 0 0.65em; }
                h1 { font-size: 1.85em; margin-top: 0.25em; }
                h2 { font-size: 1.5em; padding-bottom: 0.3em; border-bottom: 1px solid ${colors.border}; }
                h3 { font-size: 1.25em; }
                p { margin: 0.85em 0; }
                a { color: ${colors.link}; text-decoration: underline; text-underline-offset: 2px; }
                del, .user-del { text-decoration: line-through; }
                blockquote { margin: 1em 0; padding: 0.15em 0 0.15em 1em; color: ${colors.muted}; border-left: 3px solid ${colors.link}; }
                pre {
                  max-width: 100%; overflow-x: auto; -webkit-overflow-scrolling: touch;
                  padding: 14px; border-radius: 12px; background: ${colors.surface};
                  border: 1px solid ${colors.border}; line-height: 1.55; white-space: pre;
                }
                code { font-family: "Roboto Mono", "Droid Sans Mono", monospace; font-size: 0.88em; color: ${colors.code}; }
                p code, li code, td code, th code {
                  padding: 0.12em 0.35em; border-radius: 5px; background: ${colors.surface};
                  border: 1px solid ${colors.border};
                }
                pre code { padding: 0; border: 0; border-radius: 0; background: transparent; color: ${colors.foreground}; font-size: 0.85em; }
                ul, ol { padding-left: 1.55em; }
                li { padding-left: 0.15em; margin: 0.25em 0; }
                li.task-list-item { list-style: none; margin-left: -1.25em; }
                hr { border: 0; border-top: 1px solid ${colors.border}; margin: 1.8em 0; }
                table {
                  display: block; width: max-content; min-width: 100%; max-width: 100%;
                  overflow-x: auto; border-collapse: collapse; margin: 1em 0;
                  -webkit-overflow-scrolling: touch;
                }
                th, td { border: 1px solid ${colors.border}; padding: 8px 10px; text-align: left; }
                th { background: ${colors.surface}; font-weight: 650; }
                img { display: block; max-width: 100%; height: auto; margin: 1em auto; border-radius: 8px; }
                input[type=checkbox] { margin-right: 0.5em; accent-color: ${colors.link}; vertical-align: middle; }
                .mermaid-diagram { display: block; width: 100%; max-width: 100%; margin: 1.2em 0; overflow-x: auto; overflow-y: hidden; text-align: left; white-space: nowrap; -webkit-overflow-scrolling: touch; overscroll-behavior-x: contain; }
                .mermaid-diagram svg { display: block; max-width: none !important; height: auto; }
                .katex-display { max-width: 100%; overflow-x: auto; overflow-y: hidden; padding: 0.35em 0; }
                .katex { font-size: 1.05em; }
              </style>
            </head>
            <body>""".trimIndent()
            )
            append(body)
            append(
                """</body>
</html>"""
            )
        }
    }


    private fun sanitizeBody(html: String): String {
        val document = Jsoup.parseBodyFragment(html)
        document.outputSettings().prettyPrint(false)
        document.select("script, iframe, object, embed, form, base, meta, link, style").remove()

        document.body().getAllElements().forEach { element ->
            element.attributes().asList().map { it.key }.forEach { key ->
                val normalizedKey = key.lowercase(Locale.ROOT)
                val normalizedValue = element.attr(key).trim().lowercase(Locale.ROOT)
                val unsafe = normalizedKey.startsWith("on") ||
                    normalizedKey in unsafeAttributes ||
                    (normalizedKey == "href" && !(normalizedValue.startsWith("https://") ||
                        normalizedValue.startsWith("http://") || normalizedValue.startsWith("mailto:") ||
                        normalizedValue.startsWith("#"))) ||
                    (normalizedKey == "src" && !normalizedValue.startsWith("data:image/"))
                if (unsafe) element.removeAttr(key)
            }
        }
        return document.body().html()
    }

    private data class CssColors(
        val background: String,
        val foreground: String,
        val muted: String,
        val surface: String,
        val border: String,
        val link: String,
        val code: String
    )
}
