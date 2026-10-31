package com.wchaoq.markdownviewer

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

class MarkdownViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUri = intent?.data

        setContent {
            val darkTheme = isSystemInDarkTheme()
            var state by remember { mutableStateOf<ViewerState>(ViewerState.Loading) }

            LaunchedEffect(incomingUri) {
                state = if (incomingUri == null) {
                    ViewerState.Error("No Markdown document was provided.")
                } else {
                    withContext(Dispatchers.IO) {
                        runCatching { loadDocument(incomingUri) }
                            .fold(
                                onSuccess = { ViewerState.Ready(it.first, it.second) },
                                onFailure = {
                                    ViewerState.Error(it.message ?: "Unable to read this document.")
                                }
                            )
                    }
                }
            }

            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(color = Color.Transparent) {
                    when (val current = state) {
                        ViewerState.Loading -> MarkdownViewerScreen(
                            title = "Markdown Viewer",
                            markdown = null
                        )
                        is ViewerState.Ready -> MarkdownViewerScreen(
                            title = current.fileName,
                            markdown = current.content
                        )
                        is ViewerState.Error -> MarkdownViewerScreen(
                            title = "Unable to open file",
                            markdown = null,
                            error = current.message
                        )
                    }
                }
            }
        }
    }

    private fun loadDocument(uri: Uri): Pair<String, String> {
        val fileName = queryDisplayName(uri)
            ?.takeIf(String::isNotBlank)
            ?: Uri.decode(uri.lastPathSegment.orEmpty()).takeIf(String::isNotBlank)
            ?: "document.md"
        val mimeType = contentResolver.getType(uri)
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
        val normalizedName = fileName.lowercase(Locale.ROOT)
        val hasMarkdownExtension = normalizedName.endsWith(".md") ||
            normalizedName.endsWith(".markdown")
        val hasMarkdownMime = mimeType == "text/markdown" || mimeType == "text/x-markdown"

        if (!hasMarkdownExtension && !hasMarkdownMime) {
            throw IOException("This file does not appear to be a Markdown document.")
        }

        val content = contentResolver.openInputStream(uri)?.use { input ->
            InputStreamReader(SizeLimitedInputStream(input, MAX_DOCUMENT_BYTES), Charsets.UTF_8).use { reader ->
                val result = StringBuilder()
                val buffer = CharArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = reader.read(buffer)
                    if (count < 0) break
                    result.append(buffer, 0, count)
                }
                result.toString().removePrefix("\uFEFF")
            }
        } ?: throw IOException("The selected document could not be opened.")

        return fileName to content
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

    private class SizeLimitedInputStream(
        input: InputStream,
        private val maxBytes: Int
    ) : FilterInputStream(input) {
        private var bytesRead = 0

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) addBytes(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            val remainingPlusOne = (maxBytes - bytesRead + 1).coerceAtLeast(1)
            val count = super.read(buffer, offset, minOf(length, remainingPlusOne))
            if (count > 0) addBytes(count)
            return count
        }

        private fun addBytes(count: Int) {
            bytesRead += count
            if (bytesRead > maxBytes) {
                throw IOException("This Markdown document is larger than 20 MB.")
            }
        }
    }

    private sealed interface ViewerState {
        data object Loading : ViewerState
        data class Ready(val fileName: String, val content: String) : ViewerState
        data class Error(val message: String) : ViewerState
    }

    private companion object {
        const val MAX_DOCUMENT_BYTES = 20 * 1024 * 1024
    }
}
