package com.atmiya.innovation.ui.dashboard.news

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Error
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsWebViewScreen(
    url: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("News Article") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Always fallback option
                    IconButton(onClick = {
                        val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(browserIntent)
                    }) {
                         Icon(
                             imageVector = androidx.compose.material.icons.Icons.Default.OpenInBrowser,
                             contentDescription = "Open in Browser"
                         )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isError) {
                 Column(
                     modifier = Modifier.fillMaxSize().padding(24.dp),
                     verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                     horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                 ) {
                     Icon(
                         imageVector = androidx.compose.material.icons.Icons.Default.Error,
                         contentDescription = null,
                         tint = MaterialTheme.colorScheme.error,
                         modifier = Modifier.size(64.dp)
                     )
                     androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                     Text(
                         "Unable to load article",
                         style = MaterialTheme.typography.titleLarge,
                         fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                     )
                     androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                     Text(
                         "This website might be blocking access or is unavailable.",
                         style = MaterialTheme.typography.bodyMedium,
                         color = androidx.compose.ui.graphics.Color.Gray,
                         textAlign = androidx.compose.ui.text.style.TextAlign.Center
                     )
                     androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
                     Button(
                         onClick = {
                             val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                             context.startActivity(browserIntent)
                         }
                     ) {
                         Text("Open in Browser")
                     }
                 }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    isLoading = true
                                    isError = false
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }
                                override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                    // Only show error for main frame
                                    if (request?.isForMainFrame == true) {
                                        isError = true
                                        isLoading = false
                                    }
                                }
                                override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                     // Only show error for main frame
                                    if (request?.isForMainFrame == true) {
                                        isError = true
                                        isLoading = false
                                    }
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            loadUrl(url)
                        }
                    },
                    update = { webView ->
                        // Avoid reloading on recomposition
                    }
                )
            
                if (isLoading) {
                     LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = com.atmiya.innovation.ui.theme.AtmiyaPrimary
                    )
                }
            }
        }
    }
}
