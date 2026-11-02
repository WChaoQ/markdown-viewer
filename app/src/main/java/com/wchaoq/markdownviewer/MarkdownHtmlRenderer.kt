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
        val (protectedSource, mathExpressions) = protectMathExpressions(source)

        val flavour = GFMFlavourDescriptor()
        val tree = MarkdownParser(flavour).buildMarkdownTreeFromString(protectedSource)
        val sanitizedHtml = sanitizeBody(
            HtmlGenerator(protectedSource, tree, flavour).generateHtml()
        )
        return ParsedMarkdown(restoreMathExpressions(sanitizedHtml, mathExpressions))
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
              <link rel="stylesheet" href="/assets/markdown/highlight/styles/${if (darkTheme) "github-dark" else "github"}.min.css">
              <script defer src="/assets/markdown/highlight.min.js"></script>
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
                .code-toolbar {
                  display: flex; align-items: center; justify-content: space-between;
                  gap: 12px; margin: -14px -14px 12px; padding: 7px 12px;
                  border-bottom: 1px solid ${colors.border}; white-space: normal;
                  font-family: -apple-system, BlinkMacSystemFont, "Roboto", sans-serif;
                }
                .code-language {
                  color: ${colors.muted}; font-family: inherit; font-size: 12px;
                  font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em;
                }
                .code-copy {
                  appearance: none; border: 1px solid ${colors.border};
                  border-radius: 7px; padding: 4px 10px; color: ${colors.foreground};
                  background: ${colors.background}; font: inherit; font-size: 12px;
                  line-height: 1.5; cursor: pointer;
                }
                .code-copy.copied { color: ${colors.link}; border-color: ${colors.link}; }
                pre code.hljs { padding: 0; background: transparent; }
                pre code { white-space: pre; }
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
                .table-scroll {
                  display: block; width: 100%; max-width: 100%; overflow-x: auto;
                  margin: 1em 0; -webkit-overflow-scrolling: touch;
                  overscroll-behavior-x: contain;
                }
                table {
                  display: table; width: max-content; min-width: 100%; max-width: none;
                  table-layout: auto; border-collapse: collapse; margin: 0;
                }
                th, td {
                  border: 1px solid ${colors.border}; padding: 8px 10px; text-align: left;
                  white-space: nowrap; overflow-wrap: normal; word-break: normal;
                }
                th[align="left"], td[align="left"], th[style*="left"], td[style*="left"] { text-align: left; }
                th[align="center"], td[align="center"], th[style*="center"], td[style*="center"] { text-align: center; }
                th[align="right"], td[align="right"], th[style*="right"], td[style*="right"] { text-align: right; }
                th { background: ${colors.surface}; font-weight: 650; }
                img { display: block; max-width: 100%; height: auto; margin: 1em auto; border-radius: 8px; }
                input[type=checkbox] { margin-right: 0.5em; accent-color: ${colors.link}; vertical-align: middle; }
                .mermaid-diagram { display: block; width: 100%; max-width: 100%; margin: 1.2em 0; overflow-x: auto; overflow-y: hidden; text-align: left; white-space: nowrap; -webkit-overflow-scrolling: touch; overscroll-behavior-x: contain; }
                .mermaid-diagram svg { display: block; max-width: none !important; height: auto; overflow: visible; }
                .mermaid-diagram svg text, .mermaid-diagram foreignObject, .mermaid-diagram foreignObject * {
                  overflow-wrap: normal !important; word-break: normal !important;
                }
                .mermaid-diagram foreignObject div { white-space: nowrap !important; }
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

    // Display math may span lines. Inline math must never span a newline:
    // otherwise currency values in adjacent GFM table rows (for example
    // "$10,000" and "$9,500") are mistaken for one enormous math expression.
    //
    // Also: a lone "$$" in ordinary prose (e.g. a heading) must not start a
    // match that swallows the rest of the document. Block-form $$ is detected
    // by a line scan before the regex pass. Blockquote prefixes (> $$) are
    // recognized and stripped so KaTeX does not see them.
    private val displayMathPattern = Regex(
        """(?s)\\\[.*?\\\]|(?<!\\)\$\$.*?(?<!\\)\$\$"""
    )
    private val inlineMathPattern = Regex(
        """\\\(.*?\\\)|(?<!\\)\$(?!\$)[^\r\n$]+?(?<!\s)(?<!\\)\$(?!\$)"""
    )

    private fun stripBlockquoteMarkers(line: String): String {
        var rest = line
        while (true) {
            val trimmed = rest.trimStart()
            if (!trimmed.startsWith(">")) break
            rest = trimmed.removePrefix(">").let { if (it.startsWith(" ")) it.substring(1) else it }
        }
        return rest
    }

    private fun isDollarFenceLine(line: String): Boolean =
        stripBlockquoteMarkers(line).trim() == "$$"

    private fun protectBlockDollarMath(markdown: String, expressions: MutableList<String>): String {
        val lines = markdown.split('\n')
        val out = StringBuilder()
        var i = 0
        while (i < lines.size) {
            if (isDollarFenceLine(lines[i])) {
                var j = i + 1
                var closed = false
                while (j < lines.size) {
                    if (isDollarFenceLine(lines[j])) {
                        closed = true
                        break
                    }
                    j++
                }
                if (closed) {
                    val cleaned = (i..j).joinToString("\n") { idx ->
                        stripBlockquoteMarkers(lines[idx])
                    }
                    val token = "MARKDOWNVIEWERMATH${expressions.size}TOKEN"
                    expressions += cleaned
                    out.append(token)
                    i = j + 1
                    if (i < lines.size) out.append('\n')
                    continue
                }
            }
            out.append(lines[i])
            if (i + 1 < lines.size) out.append('\n')
            i++
        }
        return out.toString()
    }

    // Mask code before looking for math delimiters. Code examples commonly contain
    // shell variables, template strings, currency symbols, and LaTeX-looking text;
    // none of those should be interpreted as document mathematics.
    private val fencedCodePattern = Regex(
        """(?m)^ {0,3}(`{3,}|~{3,})[^\\n]*\\n[\\s\\S]*?^ {0,3}\\1[ \\t]*$"""
    )
    private val inlineCodePattern = Regex(
        """(?<!`) `+[^\\n`]*`+(?!`)""".replace(" ", "")
    )

    private fun protectCodeSegments(markdown: String, codeSegments: MutableList<String>): String {
        fun replaceCode(source: String, pattern: Regex): String =
            pattern.replace(source) { match ->
                val token = "MARKDOWNVIEWERCODE${codeSegments.size}TOKEN"
                codeSegments += match.value
                token
            }

        val withoutFencedCode = replaceCode(markdown, fencedCodePattern)
        return replaceCode(withoutFencedCode, inlineCodePattern)
    }

    private fun restoreCodeSegments(source: String, codeSegments: List<String>): String {
        var restored = source
        codeSegments.forEachIndexed { index, code ->
            restored = restored.replace("MARKDOWNVIEWERCODE${index}TOKEN", code)
        }
        return restored
    }

    private fun protectMathExpressions(markdown: String): Pair<String, List<String>> {
        val expressions = mutableListOf<String>()
        val codeSegments = mutableListOf<String>()

        fun replaceMatches(source: String, pattern: Regex): String =
            pattern.replace(source) { match ->
                val token = "MARKDOWNVIEWERMATH${expressions.size}TOKEN"
                expressions += match.value
                token
            }

        val withoutCode = protectCodeSegments(markdown, codeSegments)
        val withBlocks = protectBlockDollarMath(withoutCode, expressions)
        val withDisplayMath = replaceMatches(withBlocks, displayMathPattern)
        val withInlineMath = replaceMatches(withDisplayMath, inlineMathPattern)
        return restoreCodeSegments(withInlineMath, codeSegments) to expressions
    }

    private fun restoreMathExpressions(html: String, expressions: List<String>): String {
        var restored = html
        expressions.forEachIndexed { index, expression ->
            val token = "MARKDOWNVIEWERMATH${index}TOKEN"
            restored = restored.replace(token, org.jsoup.nodes.TextNode(expression).outerHtml())
        }
        return restored
    }

    private val safeTextAlignment = Regex("""text-align\s*:\s*(left|center|right)\s*;?""")

    private fun sanitizeBody(html: String): String {
        val document = Jsoup.parseBodyFragment(html)
        document.outputSettings().prettyPrint(false)
        document.select("script, iframe, object, embed, form, base, meta, link, style").remove()

        document.body().getAllElements().forEach { element ->
            element.attributes().asList().map { it.key }.forEach { key ->
                val normalizedKey = key.lowercase(Locale.ROOT)
                val normalizedValue = element.attr(key).trim().lowercase(Locale.ROOT)
                val unsafeStyle = normalizedKey == "style" &&
                    !safeTextAlignment.matches(normalizedValue)
                val unsafe = normalizedKey.startsWith("on") ||
                    (normalizedKey in unsafeAttributes && normalizedKey != "style") ||
                    unsafeStyle ||
                    (normalizedKey == "href" && !(normalizedValue.startsWith("https://") ||
                        normalizedValue.startsWith("http://") || normalizedValue.startsWith("mailto:") ||
                        normalizedValue.startsWith("#"))) ||
                    (normalizedKey == "src" && !normalizedValue.startsWith("data:image/"))
                if (unsafe) element.removeAttr(key)
            }
        }
        document.select("table").forEach { table ->
            table.wrap("<div class=\"table-scroll\"></div>")
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
