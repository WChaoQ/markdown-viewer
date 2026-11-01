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
import androidx.webkit.WebViewAssetLoader
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarkdownViewerScreen(
    title: String,
    markdown: String?,
    documentUri: Uri?
) {
    val context = LocalContext.current
    val activity = context.findActivity()\n    val assetLoader = remember(context) {\n        WebViewAssetLoader.Builder()\n            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))\n            .build()\n    }
    val darkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val background = MaterialTheme.colorScheme.surface
    var parsedMarkdown by remember(markdown) { mutableStateOf<ParsedMarkdown?>(null) }
    var renderedHtml by remember(markdown, darkTheme) { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeMatch by remember { mutableIntStateOf(0) }
    var matchCount by remember { mutableIntStateOf(0) }
    var pendingScrollY by remember { mutableIntStateOf(0) }
    var pageReady by remember { mutableStateOf(false) }
    var aboutDialogVisible by remember { mutableStateOf(false) }

    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = background.toArgb()
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    LaunchedEffect(markdown) {
        parsedMarkdown = if (markdown == null) null else {
            withContext(Dispatchers.Default) {
                MarkdownHtmlRenderer.parse(markdown)
            }
        }
    }

    LaunchedEffect(parsedMarkdown, darkTheme) {
        val document = parsedMarkdown
        renderedHtml = if (document == null) null else {
            withContext(Dispatchers.Default) {
                MarkdownHtmlRenderer.render(document, darkTheme)
            }
        }
    }

    LaunchedEffect(searchMode, searchQuery, webView, pageReady) {
        val currentWebView = webView ?: return@LaunchedEffect
        if (!pageReady) return@LaunchedEffect
        if (searchMode && searchQuery.isNotBlank()) {
            currentWebView.clearMatches()
            activeMatch = 0
            matchCount = 0
            delay(180)
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
                    IconButton(onClick = { activity?.finishAndRemoveTask() }) {
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
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("搜索文档") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    webView?.clearMatches()
                                    searchMode = false
                                    searchQuery = ""
                                    activeMatch = 0
                                    matchCount = 0
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "关闭搜索")
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium
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
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "第${if (matchCount == 0) 0 else activeMatch}项",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                            Text(
                                text = "共${matchCount}项",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
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
                    } else {
                        IconButton(onClick = {
                            searchMode = true
                            searchQuery = ""
                            activeMatch = 0
                            matchCount = 0
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_document_search),
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
                            if (isDoneCounting && pageReady && searchMode && searchQuery.isNotBlank()) {
                                activeMatch = if (numberOfMatches == 0) 0 else active + 1
                                matchCount = numberOfMatches
                            }
                        }
                        setBackgroundColor(background.toArgb())
                        settings.javaScriptEnabled = true
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

                        webViewClient = object : WebViewClient() {\n                            override fun shouldInterceptRequest(\n                                view: WebView,\n                                request: WebResourceRequest\n                            ) = assetLoader.shouldInterceptRequest(request.url)\n
                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                val loadedDocument = view.tag as? LoadedDocument ?: return
                                if (url != loadedDocument.baseUrl) return

                                pageReady = true
                                val targetScrollY = pendingScrollY
                                view.post {
                                    // Ignore a delayed callback if another document load has started.
                                    if ((view.tag as? LoadedDocument)?.baseUrl != loadedDocument.baseUrl) {
                                        return@post
                                    }
                                    val maxScrollY = (view.contentHeight * view.scale - view.height)
                                        .toInt()
                                        .coerceAtLeast(0)
                                    view.scrollTo(0, targetScrollY.coerceAtMost(maxScrollY))
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean = handleLink(view, request.url)

                            @Suppress("DEPRECATION")
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                url: String
                            ): Boolean = handleLink(view, Uri.parse(url))

                            private fun handleLink(view: WebView, uri: Uri): Boolean {
                                val currentDocumentUrl = (view.tag as? LoadedDocument)
                                    ?.baseUrl
                                    ?.let(Uri::parse)
                                if (
                                    currentDocumentUrl != null &&
                                    uri.scheme == currentDocumentUrl.scheme &&
                                    uri.host == currentDocumentUrl.host &&
                                    uri.path == currentDocumentUrl.path &&
                                    uri.fragment != null &&
                                    uri.query == null
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
                    val loadedDocument = view.tag as? LoadedDocument
                    if (html != null && loadedDocument?.html != html) {
                        pendingScrollY = view.scrollY
                        pageReady = false
                        activeMatch = 0
                        matchCount = 0
                        view.clearMatches()
                        val nextGeneration = (loadedDocument?.generation ?: 0) + 1
                        val baseUrl = "$LOCAL_DOCUMENT_BASE_URL$nextGeneration/"
                        view.tag = LoadedDocument(html, nextGeneration, baseUrl)
                        view.loadDataWithBaseURL(
                            baseUrl,
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
                    view.clearMatches()
                    view.setFindListener(null)
                    view.webViewClient = WebViewClient()
                    view.destroy()
                }
            )
        }
    }

    if (aboutDialogVisible) {
        val uriHandler = LocalUriHandler.current
        Dialog(onDismissRequest = { aboutDialogVisible = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("关于此查看器", style = MaterialTheme.typography.titleLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("版本", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("构建时间", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(BuildConfig.BUILD_TIME, style = MaterialTheme.typography.bodyLarge)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("GitHub 仓库", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "github.com/WChaoQ/markdown-viewer",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/WChaoQ/markdown-viewer")
                            }
                        )
                    }
                }
            }
        }
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

private data class LoadedDocument(
    val html: String,
    val generation: Int,
    val baseUrl: String
)

private const val LOCAL_DOCUMENT_HOST = "markdown-viewer.invalid"
private const val LOCAL_DOCUMENT_BASE_URL = "https://markdown-viewer.invalid/document/"
