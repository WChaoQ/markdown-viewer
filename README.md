# Markdown Viewer

A lightweight, read-only Android app for opening Markdown documents from other apps. It has no launcher icon or home screen: Android launches it when a compatible app or file provider sends it a Markdown document.

> **Created and built with ChatGPT (OpenAI).** The application code, UI, Markdown rendering integration, tests, and GitHub Actions build workflow were produced and refined with ChatGPT under the project owner's direction. The signed Release APK is built and verified by GitHub Actions.

## Features

- Open `.md` and `.markdown` files from Android file managers, cloud drives, messaging apps, and other apps that provide a document URI.
- Read-only experience: the source file is never edited, renamed, deleted, or copied into app storage.
- Standard Markdown and GitHub Flavored Markdown (GFM), including tables, task lists, strikethrough, and fenced code blocks.
- Syntax highlighting for common programming languages, with a one-tap code-copy action.
- LaTeX mathematics rendered by KaTeX:
  - Inline: `$...$` and `\\(...\\)`
  - Display: `$$...$$` and `\\[...\\]`
- Mermaid fenced code blocks rendered as diagrams.
- Light and dark appearance following the Android system theme.
- In-document text search, previous/next match navigation, share, open-with, and print actions.
- Offline reading: renderer scripts, styles, and fonts are bundled in the APK. The app does not need network access to display a document.

## How to use

1. Open a Markdown file in an Android file manager or another app.
2. Choose **Markdown Viewer** from the available apps.
3. Read, search, copy code, print, or share the original document using the top-bar menu.
4. Tap the back arrow to exit the viewer.

The app is intentionally a viewer, not an editor or file manager. It does not maintain a library, reading history, or persistent copy of opened documents.

## Supported input and limits

- File extensions: `.md`, `.markdown`
- MIME types: `text/markdown`, `text/x-markdown`
- `text/plain` is accepted when the document name has a supported Markdown extension.
- Maximum document size: **20 MB**.
- Text encoding: UTF-8. A UTF-8 byte-order mark is handled; malformed UTF-8 is reported as an unsupported document.

Document providers do not always report MIME types consistently, so the viewer checks both the supplied display name and MIME type.

## Rendering and security

The app uses [JetBrains Markdown](https://github.com/JetBrains/markdown) with its GFM flavour to parse Markdown, then displays sanitized HTML in a restricted Android WebView.

- Raw HTML is sanitized before display.
- A restrictive Content Security Policy allows only app-bundled scripts and styles, local fonts, and embedded `data:` images.
- WebView network loads, file access, content access, geolocation, mixed content, and pop-up windows are disabled.
- HTTP(S) and email links are opened externally; unsupported URL schemes are ignored.
- The app does not load remote images or resolve local relative image paths. This avoids unexpected network access, broad storage access, or copying adjacent files.
- JavaScript is used only for bundled rendering features such as syntax highlighting, KaTeX, Mermaid, and code-copy controls.

### Rendering libraries

| Component | Purpose |
|---|---|
| [JetBrains Markdown](https://github.com/JetBrains/markdown) | Markdown and GFM parsing |
| [Highlight.js](https://highlightjs.org/) | Code syntax highlighting |
| [KaTeX](https://katex.org/) | LaTeX mathematics |
| [Mermaid](https://mermaid.js.org/) | Diagrams |
| [Jsoup](https://jsoup.org/) | Sanitizing generated HTML |
| [AndroidX WebKit](https://developer.android.com/jetpack/androidx/releases/webkit) | Serving bundled assets safely to WebView |
| [Jetpack Compose](https://developer.android.com/compose) | Native Android interface |

The corresponding third-party license notices are included in the app assets.

## Build

### Local Debug build

Requirements:

- JDK 17
- Gradle 8.13
- Android SDK platform 36 and Build Tools 36.0.0

Run:

```bash
gradle assembleDebug
```

The APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

The build downloads the pinned renderer assets and packages them into the APK. Once built, document rendering itself works offline.

### Signed Release build

The repository includes a manually triggered GitHub Actions workflow:

**Actions → Android Release → Run workflow**

It requires these repository Actions secrets:

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: release key alias.
- `ANDROID_KEY_PASSWORD`: private-key password.

Add them under **Settings → Secrets and variables → Actions**. Never commit the keystore or passwords to the repository.

The workflow:

1. Runs the unit tests.
2. Builds Debug and Release APKs.
3. Verifies the Release APK signature.
4. Uploads the Debug APK and signed Release APK as workflow artifacts.

The latest build and downloadable artifacts are available from [GitHub Actions](https://github.com/WChaoQ/markdown-viewer/actions/workflows/android.yml). The signed Release artifact is retained for 90 days; the Debug artifact for 30 days.

Local Release builds are unsigned unless the signing environment variables are supplied.

## Privacy and permissions

The app does not request Internet, storage, or notification permissions. It does not use a database, analytics, background services, boot receivers, or persistent URI permissions.

Android grants temporary access to the selected document URI for the viewing session. The app reads the content into memory and does not retain a copy in its private storage.

## Project status

**Version 1.0.0 — ready for everyday read-only use.**

The project prioritizes reliable document opening, readable rendering, offline operation, and a small feature set over editing, synchronization, file management, or other features outside its viewer purpose.

## License

The application source is licensed under the Apache License 2.0. Third-party libraries and bundled assets retain their respective licenses; see the included license notices.
