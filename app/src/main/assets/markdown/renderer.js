(() => {
  "use strict";
  const renderDiagrams = async () => {
    const blocks = Array.from(document.querySelectorAll(
      "pre > code.language-mermaid, pre > code.lang-mermaid, pre > code[class*='language-mermaid']"
    ));
    if (blocks.length === 0 || !window.mermaid) return;
    window.mermaid.initialize({
      startOnLoad: false,
      securityLevel: "strict",
      theme: getComputedStyle(document.documentElement).colorScheme === "dark" ? "dark" : "default",
      flowchart: { htmlLabels: false }
    });
    for (let index = 0; index < blocks.length; index += 1) {
      const code = blocks[index];
      const source = code.textContent || "";
      const host = document.createElement("div");
      host.className = "mermaid-diagram";
      code.parentElement.replaceWith(host);
      try {
        const result = await window.mermaid.render("markdown-viewer-diagram-" + index, source);
        host.innerHTML = result.svg;
        if (typeof result.bindFunctions === "function") result.bindFunctions(host);
      } catch (_) {
        const pre = document.createElement("pre");
        const fallback = document.createElement("code");
        fallback.textContent = source;
        pre.appendChild(fallback);
        host.replaceWith(pre);
      }
    }
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
        ignoredTags: ["script", "noscript", "style", "textarea", "pre", "code", "option"],
        ignoredClasses: ["mermaid-diagram"],
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
    renderMath();
  };
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", render, { once: true });
  } else {
    render();
  }
})();
