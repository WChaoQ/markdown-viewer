package com.wchaoq.markdownviewer

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkdownViewerScreen(
    title: String,
    markdown: String?,
    error: String? = null
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val background = if (darkTheme) Color(0xFF121212) else Color(0xFFFFFBFE)
    var renderedHtml by remember(markdown, darkTheme) { mutableStateOf<String?>(null) }

    LaunchedEffect(markdown, darkTheme) {
        renderedHtml = if (markdown == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                MarkdownHtmlRenderer.render(markdown, darkTheme)
            }
        }
    }

    Scaffold(
        containerColor = background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = background
                )
            )
        }
    ) { innerPadding ->
        when {
            error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(error)
            }

            markdown == null || renderedHtml == null -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            else -> AndroidView(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        setBackgroundColor(
                            AndroidColor.parseColor(if (darkTheme) "#121212" else "#FFFBFE")
                        )
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.blockNetworkLoads = true
                        settings.loadsImagesAutomatically = true
                        overScrollMode = View.OVER_SCROLL_NEVER
                        isVerticalScrollBarEnabled = false

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean = handleLink(request.url)

                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                url: String
                            ): Boolean = handleLink(Uri.parse(url))

                            private fun handleLink(uri: Uri): Boolean {
                                // The synthetic base URL keeps same-document heading links local.
                                if (
                                    uri.host == LOCAL_DOCUMENT_HOST &&
                                    uri.fragment != null &&
                                    (uri.path.isNullOrEmpty() || uri.path == "/")
                                ) return false

                                return when (uri.scheme?.lowercase()) {
                                    "http", "https", "mailto" -> {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                        true
                                    }
                                    else -> true
                                }
                            }
                        }
                    }
                },
                update = { webView ->
                    val html = renderedHtml
                    if (html != null && webView.tag !== html) {
                        webView.tag = html
                        webView.loadDataWithBaseURL(
                            LOCAL_DOCUMENT_BASE_URL,
                            html,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.webViewClient = WebViewClient()
                    webView.destroy()
                }
            )
        }
    }
}

private const val LOCAL_DOCUMENT_HOST = "markdown-viewer.invalid"
private const val LOCAL_DOCUMENT_BASE_URL = "https://markdown-viewer.invalid/"
