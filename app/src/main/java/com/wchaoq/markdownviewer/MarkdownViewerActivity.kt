package com.wchaoq.markdownviewer

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class MarkdownViewerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUri = intent?.data

        setContent {
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

            MaterialTheme {
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
        val fileName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "document.md"
        val mimeType = contentResolver.getType(uri)
        val hasMarkdownExtension = fileName.endsWith(".md", ignoreCase = true) ||
            fileName.endsWith(".markdown", ignoreCase = true)
        val hasMarkdownMime = mimeType == "text/markdown" || mimeType == "text/x-markdown"

        if (!hasMarkdownExtension && !hasMarkdownMime) {
            throw IOException("This file does not appear to be a Markdown document.")
        }

        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0

            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_DOCUMENT_BYTES) {
                    throw IOException("This Markdown document is larger than 20 MB.")
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw IOException("The selected document could not be opened.")

        val content = bytes.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
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

    private sealed interface ViewerState {
        data object Loading : ViewerState
        data class Ready(val fileName: String, val content: String) : ViewerState
        data class Error(val message: String) : ViewerState
    }

    private companion object {
        const val MAX_DOCUMENT_BYTES = 20 * 1024 * 1024
    }
}
