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
      flowchart: { htmlLabels: false },
      gantt: {
        useMaxWidth: false,
        leftPadding: 180,
        barHeight: 24,
        barGap: 8,
        topPadding: 50,
        fontSize: 14,
        sectionFontSize: 14,
        // A weekly axis is readable on compact screens; daily ticks overlap
        // badly on short project timelines.
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
        const result = await window.mermaid.render("markdown-viewer-diagram-" + index, source);
        host.innerHTML = result.svg;
        // Mermaid often emits SVG with max-width: 100%, which scales large
        // diagrams down to the viewport and prevents horizontal panning.
        // Restore the SVG's viewBox dimensions so the scroll container can
        // expose the complete diagram on narrow screens.
        const svg = host.querySelector("svg");
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
