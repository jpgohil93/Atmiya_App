package com.atmiya.innovation.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done // Replaced DoneAll
import androidx.compose.material.icons.filled.PlayArrow // Replaced Videocam
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Mock Message Model
data class ChatMessage(
    val id: String,
    val text: String,
    val isMe: Boolean,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageStatus { SENT, DELIVERED, READ }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    userId: String,
    userName: String, // Passed for header
    userPhotoUrl: String, 
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    // Mock Messages
    val messages = remember { mutableStateListOf(
        ChatMessage("1", "Hello!", false, System.currentTimeMillis() - 3600000, MessageStatus.READ),
        ChatMessage("2", "Hi there, how are you?", true, System.currentTimeMillis() - 3500000, MessageStatus.READ),
        ChatMessage("3", "I'm doing good, thanks for asking.", false, System.currentTimeMillis() - 3400000, MessageStatus.READ),
        ChatMessage("4", "Great to hear!", true, System.currentTimeMillis() - 10000, MessageStatus.SENT)
    ) }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (userPhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = AtmiyaPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Online", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Default.PlayArrow, contentDescription = "Video Call") } // Changed from Videocam
                    IconButton(onClick = {}) { Icon(Icons.Default.Call, contentDescription = "Call") }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AtmiyaPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input Field Background
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                        IconButton(onClick = { /* TODO: Attach */ }) {
                             Icon(Icons.Default.PlayArrow, contentDescription = "Video Call", tint = Color.White) // Changed from Add
                        }
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Message") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Mic / Send Button
                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            messages.add(ChatMessage(java.util.UUID.randomUUID().toString(), messageText, true, System.currentTimeMillis()))
                            messageText = ""
                        }
                    },
                    containerColor = AtmiyaPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (messageText.isBlank()) Icons.Default.Add else Icons.AutoMirrored.Filled.Send, // Should be Mic usually, but let's stick to Send logic
                        contentDescription = "Send",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        // Chat Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEFE7DE)) // WhatsApp-like light beige default
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isMe) Color(0xFFE7FFDB) else Color.White // WhatsApp green for me, White for them
    val align = if (message.isMe) Alignment.End else Alignment.Start
    val shape = if (message.isMe) RoundedCornerShape(8.dp, 0.dp, 8.dp, 8.dp) else RoundedCornerShape(0.dp, 8.dp, 8.dp, 8.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    if (message.isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                             imageVector = if (message.status == MessageStatus.READ) Icons.Default.Done else Icons.Default.Check,
                             contentDescription = null,
                             tint = if (message.status == MessageStatus.READ) Color(0xFF53BDEB) else Color.Gray, // Blue ticks
                             modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
