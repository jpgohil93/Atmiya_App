package com.atmiya.innovation.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.atmiya.innovation.ui.theme.AtmiyaPrimary
import com.atmiya.innovation.ui.theme.AtmiyaSecondary

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val type: MessageType = MessageType.TEXT,
    val options: List<String>? = null
)

enum class MessageType {
    TEXT, OPTIONS, FILE_UPLOAD
}

@Composable
fun ChatOnboardingScreen(
    role: String,
    startupType: String? = null,
    onOnboardingComplete: () -> Unit
) {
    val manager = remember { com.atmiya.innovation.logic.OnboardingManager(role, startupType) }
    
    // Initial state with the first question
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    
    // Load first question on start
    LaunchedEffect(Unit) {
        val firstQ = manager.getNextQuestion()
        if (firstQ != null) {
            messages = listOf(ChatMessage("Welcome! Let's get your profile set up.", false), firstQ)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
    
    // File Picker
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            // Upload File
            val ref = storage.reference.child("uploads/${java.util.UUID.randomUUID()}")
            ref.putFile(uri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        val url = downloadUri.toString()
                        // Send URL as message
                        val newMessages = messages.toMutableList()
                        newMessages.add(ChatMessage("File Uploaded: $url", true))
                        
                        // Process answer
                        val nextQ = manager.processAnswer(url)
                        if (nextQ != null) {
                            newMessages.add(nextQ)
                            if (nextQ.text.startsWith("Thanks!")) {
                                // onOnboardingComplete()
                            }
                        } else {
                             onOnboardingComplete()
                        }
                        messages = newMessages
                    }
                }
                .addOnFailureListener {
                    android.widget.Toast.makeText(context, "Upload Failed", android.widget.Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Chat List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Check if current question expects a file (Hack for MVP: Check text)
            val currentQ = messages.lastOrNull { !it.isUser }
            if (currentQ?.text?.contains("upload", ignoreCase = true) == true || currentQ?.text?.contains("deck", ignoreCase = true) == true) {
                 Button(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Select File")
                }
            } else {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type your answer...") },
                    shape = RoundedCornerShape(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (currentQ?.text?.contains("upload", ignoreCase = true) != true && currentQ?.text?.contains("deck", ignoreCase = true) != true) {
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            // Add user message
                            val newMessages = messages.toMutableList()
                            newMessages.add(ChatMessage(inputText, true))
                            
                            // Process answer
                            val nextQ = manager.processAnswer(inputText)
                            if (nextQ != null) {
                                newMessages.add(nextQ)
                                if (nextQ.text.startsWith("Thanks!")) {
                                    // Delay slightly then complete
                                    // onOnboardingComplete() // In real app, wait a bit
                                }
                            } else {
                                // End of flow
                                 onOnboardingComplete()
                            }
                            
                            messages = newMessages
                            inputText = ""
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isFromUser) AtmiyaPrimary else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 0.dp,
                bottomEnd = if (message.isFromUser) 0.dp else 16.dp
            ),
            shadowElevation = 2.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isFromUser) Color.White else Color.Black
            )
        }
    }
}
