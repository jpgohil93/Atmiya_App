package com.atmiya.innovation.ui.event

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.atmiya.innovation.data.AIFEvent
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.components.SoftCard
import com.atmiya.innovation.ui.theme.AtmiyaAccent
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    var event by remember { mutableStateOf<AIFEvent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(eventId) {
        scope.launch {
            try {
                isLoading = true
                event = repository.getAIFEvent(eventId)
                if (event == null) {
                    errorMessage = "Event not found"
                }
            } catch (e: Exception) {
                android.util.Log.e("EventDetail", "Error loading event", e)
                errorMessage = "Failed to load event details"
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            event?.let { evt ->
                                val shareText = """
                                    ${evt.title}
                                    
                                    ${formatEventDateRange(evt.startDate, evt.endDate)}
                                    ${evt.venue}, ${evt.city}
                                    
                                    ${evt.description}
                                """.trimIndent()
                                
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Event"))
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Error",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
                event != null -> {
                    EventDetailContent(
                        event = event!!,
                        onOpenMap = {
                            val query = "${event!!.venue}, ${event!!.city}"
                            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
                                )
                                context.startActivity(browserIntent)
                            }
                        },
                        onRegister = {
                            event?.registrationUrl?.let { url ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EventDetailContent(
    event: AIFEvent,
    onOpenMap: () -> Unit,
    onRegister: () -> Unit
) {
    val url = if (event.registrationUrl.isNullOrEmpty()) "https://aif.org.in/" else event.registrationUrl
    
    AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = android.webkit.WebViewClient()
                loadUrl(url)
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        },
        modifier = Modifier.fillMaxSize()
    )
}

fun formatEventDateRange(startDate: Timestamp?, endDate: Timestamp?): String {
    if (startDate == null) return "Date TBD"
    
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    val startStr = dateFormat.format(startDate.toDate())
    val fullStartStr = fullDateFormat.format(startDate.toDate())
    
    return if (endDate != null && endDate.toDate().time != startDate.toDate().time) {
        val endStr = dateFormat.format(endDate.toDate())
        val endYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(endDate.toDate())
        "$startStr - $endStr, $endYear"
    } else {
        fullStartStr
    }
}
