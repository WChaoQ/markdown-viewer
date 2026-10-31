package com.wchaoq.markdownviewer

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.print.PrintManager
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkdownViewerScreen(
    title: String,
    markdown: String?,
    documentUri: Uri?
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val background = MaterialTheme.colorScheme.surface
    var renderedHtml by remember(markdown, darkTheme) { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeMatch by remember { mutableIntStateOf(0) }
    var matchCount by remember { mutableIntStateOf(0) }
    var aboutDialogVisible by remember { mutableStateOf(false) }

    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = background.toArgb()
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    LaunchedEffect(markdown, darkTheme) {
        renderedHtml = if (markdown == null) null else {
            withContext(Dispatchers.Default) {
                MarkdownHtmlRenderer.render(markdown, darkTheme)
            }
        }
    }

    LaunchedEffect(searchMode, searchQuery, webView) {
        val currentWebView = webView ?: return@LaunchedEffect
        if (searchMode && searchQuery.isNotBlank()) {
            currentWebView.findAllAsync(searchQuery)
        } else {
            currentWebView.clearMatches()
            activeMatch = 0
            matchCount = 0
        }
    }

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "退出"
                        )
                    }
                },
                title = {
                    if (searchMode) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                activeMatch = 0
                                if (it.isBlank()) {
                                    matchCount = 0
                                    activeMatch = 0
                                }
                            },
                            modifier = Modifier.width(190.dp),
                            singleLine = true,
                            placeholder = { Text("搜索文档") },
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (searchMode) {
                        Text(
                            text = "第${if (matchCount == 0) 0 else activeMatch}项，共${matchCount}项",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                        IconButton(
                            enabled = matchCount > 0,
                            onClick = { webView?.findNext(false) }
                        ) {
                            Icon(Icons.Filled.NavigateBefore, contentDescription = "上一项")
                        }
                        IconButton(
                            enabled = matchCount > 0,
                            onClick = { webView?.findNext(true) }
                        ) {
                            Icon(Icons.Filled.NavigateNext, contentDescription = "下一项")
                        }
                        IconButton(onClick = {
                            webView?.clearMatches()
                            searchMode = false
                            searchQuery = ""
                            matchCount = 0
                            activeMatch = 0
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "关闭搜索")
                        }
                    } else {
                        IconButton(onClick = {
                            searchMode = true
                            searchQuery = ""
                            activeMatch = 0
                            matchCount = 0
                        }) {
                            Icon(
                                Icons.Filled.FindInPage,
                                contentDescription = "搜索文档"
                            )
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("发送文件") },
                                    onClick = {
                                        menuExpanded = false
                                        documentUri?.let { shareDocument(context, it, title) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("打开方式") },
                                    onClick = {
                                        menuExpanded = false
                                        documentUri?.let { openWith(context, it) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("打印") },
                                    onClick = {
                                        menuExpanded = false
                                        webView?.let { view ->
                                            runCatching {
                                                val printManager = context.getSystemService(
                                                    Context.PRINT_SERVICE
                                                ) as PrintManager
                                                printManager.print(
                                                    title,
                                                    view.createPrintDocumentAdapter(title),
                                                    null
                                                )
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("关于此查看器") },
                                    onClick = {
                                        menuExpanded = false
                                        aboutDialogVisible = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background
                )
            )
        }
    ) { innerPadding ->
        when {
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
                        webView = this
                        setFindListener { active, numberOfMatches, isDoneCounting ->
                            if (isDoneCounting) {
                                activeMatch = if (numberOfMatches == 0) 0 else active + 1
                                matchCount = numberOfMatches
                            }
                        }
                        setBackgroundColor(background.toArgb())
                        settings.javaScriptEnabled = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        settings.domStorageEnabled = false
                        settings.databaseEnabled = false
                        settings.setGeolocationEnabled(false)
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false
                        settings.blockNetworkLoads = true
                        settings.mixedContentMode =
                            android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = true
                        }
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
                update = { view ->
                    webView = view
                    val html = renderedHtml
                    if (html != null && view.tag !== html) {
                        view.tag = html
                        view.loadDataWithBaseURL(
                            LOCAL_DOCUMENT_BASE_URL,
                            html,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                onRelease = { view ->
                    webView = null
                    view.stopLoading()
                    view.webViewClient = WebViewClient()
                    view.destroy()
                }
            )
        }
    }

    if (aboutDialogVisible) {
        AlertDialog(
            onDismissRequest = { aboutDialogVisible = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = null
                )
            },
            title = { Text("Markdown Viewer") },
            text = { Text("一个简洁、只读的 Markdown 文档查看器。") },
            confirmButton = {
                TextButton(onClick = { aboutDialogVisible = false }) {
                    Text("确定")
                }
            }
        )
    }
}

private fun shareDocument(context: Context, uri: Uri, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        clipData = ClipData.newUri(context.contentResolver, title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "发送文件")) }
}

private fun openWith(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/markdown")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "打开方式")) }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

private const val LOCAL_DOCUMENT_HOST = "markdown-viewer.invalid"
private const val LOCAL_DOCUMENT_BASE_URL = "https://markdown-viewer.invalid/"
