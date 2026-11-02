(() => {
  "use strict";

  const fallbackToCode = (host, source) => {
    const pre = document.createElement("pre");
    const fallback = document.createElement("code");
    fallback.textContent = source;
    pre.appendChild(fallback);
    host.replaceWith(pre);
  };

  const isMermaidErrorSvg = (svg) => {
    if (!svg) return true;
    const text = (svg.textContent || "").toLowerCase();
    return text.includes("syntax error") || text.includes("error in text");
  };

  const renderDiagrams = async () => {
    const blocks = Array.from(document.querySelectorAll(
      "pre > code.language-mermaid, pre > code.lang-mermaid, pre > code[class*='language-mermaid']"
    ));
    if (blocks.length === 0 || !window.mermaid) return;
    window.mermaid.initialize({
      startOnLoad: false,
      securityLevel: "strict",
      theme: getComputedStyle(document.documentElement).colorScheme === "dark" ? "dark" : "default",
      flowchart: { htmlLabels: false },
      gantt: {
        useMaxWidth: false,
        leftPadding: 180,
        barHeight: 24,
        barGap: 8,
        topPadding: 50,
        fontSize: 14,
        sectionFontSize: 14,
        tickInterval: "1week",
        axisFormat: "%m-%d"
      }
    });

    for (let index = 0; index < blocks.length; index += 1) {
      const code = blocks[index];
      const source = code.textContent || "";
      const host = document.createElement("div");
      host.className = "mermaid-diagram";
      code.parentElement.replaceWith(host);
      try {
        if (typeof window.mermaid.parse === "function") {
          await window.mermaid.parse(source);
        }
        const result = await window.mermaid.render("markdown-viewer-diagram-" + index, source);
        host.innerHTML = result.svg;
        const svg = host.querySelector("svg");
        if (isMermaidErrorSvg(svg)) {
          fallbackToCode(host, source);
          continue;
        }
        if (svg) {
          const viewBox = svg.viewBox && svg.viewBox.baseVal;
          if (viewBox && viewBox.width > 0 && viewBox.height > 0) {
            svg.style.setProperty("width", viewBox.width + "px", "important");
            svg.style.setProperty("height", viewBox.height + "px", "important");
          }
          svg.style.setProperty("max-width", "none", "important");
          svg.style.setProperty("display", "block", "important");
        }
        if (typeof result.bindFunctions === "function") result.bindFunctions(host);
      } catch (_) {
        fallbackToCode(host, source);
      }
    }
  };

  const languageFromClass = (code) => {
    const match = Array.from(code.classList).find((name) =>
      name.startsWith("language-") || name.startsWith("lang-")
    );
    if (!match) return "";
    const language = match.replace(/^(language|lang)-/, "").toLowerCase();
    const aliases = {
      js: "javascript", jsx: "javascript", ts: "typescript", tsx: "typescript",
      html: "xml", xml: "xml", sh: "bash", shell: "bash", zsh: "bash",
      "c++": "cpp", "c#": "csharp",
      yml: "yaml", kt: "kotlin", kts: "kotlin",
      md: "markdown", py: "python"
    };
    return aliases[language] || language;
  };

  const copyText = async (text) => {
    if (navigator.clipboard && typeof navigator.clipboard.writeText === "function") {
      await navigator.clipboard.writeText(text);
      return;
    }
    const textarea = document.createElement("textarea");
    textarea.value = text;
    textarea.setAttribute("readonly", "");
    textarea.style.position = "fixed";
    textarea.style.opacity = "0";
    document.body.appendChild(textarea);
    textarea.select();
    const copied = document.execCommand("copy");
    textarea.remove();
    if (!copied) throw new Error("Clipboard unavailable");
  };

  const renderCodeBlocks = () => {
    const highlighter = window.hljs;
    const blocks = Array.from(document.querySelectorAll("pre > code")).filter((code) =>
      !code.classList.contains("language-mermaid") &&
      !code.classList.contains("lang-mermaid")
    );

    blocks.forEach((code) => {
      const pre = code.parentElement;
      if (!pre || pre.dataset.enhanced === "true") return;
      pre.dataset.enhanced = "true";

      const rawLanguage = languageFromClass(code);
      const supportedLanguage = highlighter && rawLanguage && highlighter.getLanguage(rawLanguage);
      if (supportedLanguage) {
        try {
          highlighter.highlightElement(code);
        } catch (_) {
          // Keep the original escaped source visible if highlighting fails.
        }
      }

      const toolbar = document.createElement("div");
      toolbar.className = "code-toolbar";
      const label = document.createElement("span");
      label.className = "code-language";
      label.textContent = rawLanguage || "Code";
      const button = document.createElement("button");
      button.className = "code-copy";
      button.type = "button";
      button.textContent = "复制";
      button.setAttribute("aria-label", "复制代码");
      button.addEventListener("click", async () => {
        try {
          await copyText(code.textContent || "");
          button.textContent = "已复制";
          button.classList.add("copied");
          window.setTimeout(() => {
            button.textContent = "复制";
            button.classList.remove("copied");
          }, 1400);
        } catch (_) {
          button.textContent = "复制失败";
          window.setTimeout(() => { button.textContent = "复制"; }, 1400);
        }
      });
      toolbar.append(label, button);
      pre.insertBefore(toolbar, code);
    });
  };

  const renderMath = () => {
    if (typeof window.renderMathInElement !== "function") return;
    try {
      window.renderMathInElement(document.body, {
        delimiters: [
          { left: "$$", right: "$$", display: true },
          { left: "\\[", right: "\\]", display: true },
          { left: "\\(", right: "\\)", display: false },
          { left: "$", right: "$", display: false }
        ],
        ignoredTags: ["script", "noscript", "style", "textarea", "pre", "code", "option", "button"],
        ignoredClasses: ["mermaid-diagram", "code-toolbar"],
        throwOnError: false,
        strict: "ignore",
        trust: false
      });
    } catch (_) {
      // Keep source text visible if malformed math is encountered.
    }
  };

  const render = async () => {
    await renderDiagrams();
    renderCodeBlocks();
    renderMath();
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", render, { once: true });
  } else {
    render();
  }
})();