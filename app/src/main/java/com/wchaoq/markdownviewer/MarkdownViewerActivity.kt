package com.wchaoq.markdownviewer

import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.Locale

class MarkdownViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUri = intent?.data

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val context = LocalContext.current
            val colorScheme = remember(darkTheme, context) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    if (darkTheme) darkColorScheme() else lightColorScheme()
                }
            }

            var fileName by remember { mutableStateOf<String?>(null) }
            var document by remember { mutableStateOf<LoadedDocumentContent?>(null) }

            LaunchedEffect(incomingUri) {
                if (incomingUri == null) {
                    closeWithMessage("No document was provided.")
                    return@LaunchedEffect
                }

                try {
                    val loaded = withContext(Dispatchers.IO) {
                        loadDocument(incomingUri)
                    }
                    fileName = loaded.fileName
                    document = loaded
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    closeWithMessage(error.toUserMessage())
                }
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(color = Color.Transparent) {
                    MarkdownViewerScreen(
                        title = fileName ?: "Markdown Viewer",
                        document = document,
                        documentUri = incomingUri
                    )
                }
            }
        }
    }

    private fun closeWithMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun Exception.toUserMessage(): String = when (this) {
        is DocumentTooLargeException -> "This document is larger than 20 MB."
        is EmptyMarkdownDocumentException -> "This document is empty."
        is UnsupportedMarkdownException -> "This file type is not supported."
        is SecurityException -> "Permission to read this document was denied. Open it again."
        is FileNotFoundException -> "This document is no longer available."
        is CharacterCodingException -> "This document is not valid UTF-8 text."
        else -> "Unable to read this document. Please open it again."
    }

    private fun loadDocument(uri: Uri): LoadedDocumentContent {
        val fileName = queryDisplayName(uri)
            ?.takeIf(String::isNotBlank)
            ?: Uri.decode(uri.lastPathSegment.orEmpty()).takeIf(String::isNotBlank)
            ?: "document.md"
        val normalizedName = fileName.lowercase(Locale.ROOT)
        val kind = when {
            normalizedName.endsWith(".md") || normalizedName.endsWith(".markdown") -> DocumentKind.MARKDOWN
            normalizedName.endsWith(".txt") -> DocumentKind.PLAIN_TEXT
            normalizedName.endsWith(".svg") -> DocumentKind.SVG
            else -> throw UnsupportedMarkdownException()
        }

        val content = contentResolver.openInputStream(uri)?.use { input ->
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            InputStreamReader(SizeLimitedInputStream(input, MAX_DOCUMENT_BYTES), decoder).use { reader ->
                val result = StringBuilder(DEFAULT_BUFFER_SIZE)
                val firstCharacter = reader.read()
                if (firstCharacter >= 0 && firstCharacter != '\uFEFF'.code) {
                    result.append(firstCharacter.toChar())
                }

                val buffer = CharArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    result.append(buffer, 0, count)
                }
                result.toString()
            }
        } ?: throw IOException("Unable to open document.")

        if (content.isBlank()) throw EmptyMarkdownDocumentException()
        return LoadedDocumentContent(fileName, content, kind)
    }

    private fun queryDisplayName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            if (cursor?.moveToFirst() == true) {
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    private companion object {
        const val MAX_DOCUMENT_BYTES = 20 * 1024 * 1024
    }
}

internal enum class DocumentKind {
    MARKDOWN,
    PLAIN_TEXT,
    SVG
}

internal data class LoadedDocumentContent(
    val fileName: String,
    val content: String,
    val kind: DocumentKind
)

internal class DocumentTooLargeException : IOException()
internal class EmptyMarkdownDocumentException : IOException()
internal class UnsupportedMarkdownException : IOException()
