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

The app uses [JetBrains Markdown](https://github.com/JetBrains/markdown) (`org.jetbrains:markdown:0.7.16`) with its GFM flavour to generate HTML. A restricted WebView displays the generated document. JavaScript is enabled for the app-bundled KaTeX renderer and, only when a Mermaid block is present, the app-bundled Mermaid renderer; WebView network loads, file access, and content access remain disabled. The generated page applies a restrictive Content Security Policy: only same-origin bundled scripts and styles, local fonts, and embedded `data:` images are permitted. Frames, embedded objects, forms, remote resources, and base-URL overrides are blocked. Raw HTML from Markdown is sanitized before display. A no-referrer policy prevents document URLs from being sent as referrer information. HTTP(S) links are handed to an external browser.

The first version does not resolve local relative image paths. This avoids copying neighbouring files or requesting broad storage access. Remote images are also not loaded because the app does not request Internet access.

### Code blocks

- Highlight.js highlights fenced code blocks with a language tag, including Kotlin, Java, Python, JavaScript, TypeScript, HTML/XML, CSS, JSON, Bash, YAML, C/C++, Rust, SQL, and other bundled common languages.
- Code blocks show a language label and a copy action. Unknown or unlabelled languages remain readable as plain text.
- Highlight.js scripts and GitHub-style light/dark themes are packaged as local APK assets. Code highlighting and copying work offline.

### Mathematics and diagrams

- KaTeX renders inline math with `$...$` and `\\(...\\)`, and display math with `$$...$$` and `\\[...\\]`.
- Mermaid fenced blocks (```mermaid) render as diagrams. Invalid diagram syntax remains visible as a code block.
- Renderer scripts, styles, fonts, and license notices are packaged as local APK assets. Reading does not need network access and the app does not request Internet permission.

## Build

Use Gradle 8.13 with JDK 17:

```bash
gradle assembleDebug
```

The Debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Signed Release APK

The Release workflow requires four GitHub Actions repository secrets:

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded contents of your release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: `markdown-viewer`.
- `ANDROID_KEY_PASSWORD`: private-key password.

Never commit the keystore or passwords to the repository. Open **Settings → Secrets and variables → Actions → New repository secret** to add each value. Then open **Actions → Android Release → Run workflow**. The workflow runs unit tests, builds the signed Release APK, verifies its signature, and uploads both Debug and signed Release APK artifacts.

Release signing is applied only when the signing environment variables are present. Local builds without those values remain unsigned.

## Privacy and permissions

The manifest intentionally does not request storage, Internet, or notification permissions. The app does not declare a `MAIN` / `LAUNCHER` activity, so it does not appear in the launcher. Android's temporary URI grant is used only for the lifetime of the open activity; no persistable grant is taken.

## License

The application source is licensed under Apache-2.0. JetBrains Markdown is a separate Apache-2.0 dependency.
