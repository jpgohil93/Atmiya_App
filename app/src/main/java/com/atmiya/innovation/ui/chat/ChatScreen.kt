package com.atmiya.innovation.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.data.ChatMessage
import com.atmiya.innovation.repository.FirestoreRepository
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    channelId: String,
    otherUserName: String,
    onBack: () -> Unit
) {
    val repository = remember { FirestoreRepository() }
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()
    
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var newMessageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(channelId) {
        repository.getMessages(channelId).collectLatest {
            messages = it
            // Scroll to bottom on new message
            if (it.isNotEmpty()) {
                listState.animateScrollToItem(it.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(otherUserName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = AtmiyaPrimary
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .imePadding(), // Handle keyboard
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessageText,
                    onValueChange = { newMessageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = AtmiyaPrimary,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newMessageText.isNotBlank() && currentUser != null) {
                            val msg = ChatMessage(
                                senderId = currentUser.uid,
                                text = newMessageText
                            )
                            scope.launch {
                                repository.sendMessage(channelId, msg)
                                newMessageText = ""
                            }
                        }
                    },
                    enabled = newMessageText.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AtmiyaPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == currentUser?.uid
                MessageBubble(message = message, isMe = isMe)
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            ),
            color = if (isMe) AtmiyaPrimary else Color.LightGray.copy(alpha = 0.3f),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (isMe) Color.White else Color.Black
            )
        }
    }
}
