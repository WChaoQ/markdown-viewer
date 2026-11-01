# Markdown Viewer

A minimal, read-only Android Markdown viewer. It has no launcher icon or home screen: Android starts it only when another app asks to open a supported Markdown document.

## Principles

- Read the supplied URI for the current viewing session only.
- Never copy the document into app storage or retain URI permissions.
- Never edit, rename, delete, or export the source document.
- No database, reading history, background service, boot receiver, analytics, or network permission.
- Native Android activity and Jetpack Compose UI; JetBrains Markdown parses the document.

## Supported input

- `.md` and `.markdown`
- `text/markdown`, `text/x-markdown`
- `text/plain` when the URI path matches a Markdown extension

The MIME type supplied by document providers is not always consistent. The activity validates both the display name and MIME type after receiving the intent.

## Rendering

The app uses [JetBrains Markdown](https://github.com/JetBrains/markdown) (`org.jetbrains:markdown:0.7.16`) with its GFM flavour to generate HTML. A restricted WebView displays the generated document. JavaScript is enabled only for the app-bundled KaTeX and Mermaid renderers; WebView network loads, file access, and content access remain disabled. The generated page applies a restrictive Content Security Policy: only same-origin bundled scripts and styles, local fonts, and embedded `data:` images are permitted. Frames, embedded objects, forms, remote resources, and base-URL overrides are blocked. Raw HTML from Markdown is sanitized before display. A no-referrer policy prevents document URLs from being sent as referrer information. HTTP(S) links are handed to an external browser.

The first version does not resolve local relative image paths. This avoids copying neighbouring files or requesting broad storage access. Remote images are also not loaded because the app does not request Internet access.

## Build

Open the project in Android Studio, or use Gradle 8.13 with JDK 17:

```bash
gradle assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

Builds are manual only. Open **Actions → Android CI → Run workflow** when you explicitly want to run tests and build Debug and unsigned Release APKs. APKs are uploaded as workflow artifacts.

## Privacy and permissions

The manifest intentionally does not request storage, Internet, or notification permissions. The app does not declare a `MAIN` / `LAUNCHER` activity, so it does not appear in the launcher. Android's temporary URI grant is used only for the lifetime of the open activity; no persistable grant is taken.

## License

The application source is licensed under Apache-2.0. JetBrains Markdown is a separate Apache-2.0 dependency.


### Mathematics and diagrams

- KaTeX renders inline math with `$...$` and `\\(...\\)`, and display math with `$$...$$` and `\\[...\\]`.
- Mermaid fenced blocks (```mermaid) render as diagrams. Invalid diagram syntax remains visible as a code block.
- Renderer scripts, styles, fonts, and license notices are packaged as local APK assets. Reading does not need network access and the app does not request Internet permission.
- The first build downloads pinned renderer assets into Gradle's generated build directory; the build environment therefore needs network access.
