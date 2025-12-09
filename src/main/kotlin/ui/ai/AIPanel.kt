package ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.ai.AIProviderType
import core.ai.AISettings
import core.ai.ChatMessage
import core.ai.MessageRole
import kotlinx.coroutines.launch

@Composable
fun AIPanel(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Get current provider status
    val providerType = remember { mutableStateOf(AISettings.selectedProvider) }
    val modelName = remember { mutableStateOf(AISettings.selectedModel) }
    val accessLevel = remember { mutableStateOf(AISettings.codeAccessLevel) }
    
    val providerText = when (providerType.value) {
        AIProviderType.LOCAL -> "Basic (Local)"
        AIProviderType.GEMINI -> if (AISettings.geminiApiKey.isNotBlank()) {
            "${modelName.value} | ${accessLevel.value.name.replace("_", " ")}"
        } else {
            "Basic (Local)"
        }
        AIProviderType.OPENAI -> if (AISettings.openaiApiKey.isNotBlank()) "OpenAI Connected" else "Basic (Local)"
    }
    val providerColor = when (providerType.value) {
        AIProviderType.LOCAL -> Color(0xFF81C784) // Green
        AIProviderType.GEMINI -> Color(0xFF64B5F6) // Blue
        AIProviderType.OPENAI -> Color(0xFFBA68C8) // Purple
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // Header
        Surface(
            color = Color(0xFF2D2D2D),
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SenseDev AI",
                    style = MaterialTheme.typography.h6,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = providerText,
                    style = MaterialTheme.typography.caption,
                    color = providerColor,
                    modifier = Modifier
                        .background(providerColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                text = "Ask me about nodes, flows, or connections in your graph.",
                                color = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    items(messages) { message ->
                        MessageBubble(message)
                    }
                    
                    // Loading indicator
                    if (isLoading) {
                        item {
                            LoadingIndicator()
                        }
                    }
                }
            }
        }

        // Input Area
        Surface(
            color = Color(0xFF2D2D2D),
            elevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                                true
                            } else {
                                false
                            }
                        },
                    placeholder = { Text("Ask a question...", color = Color.Gray) },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF1E1E1E),
                        textColor = Color.White,
                        cursorColor = MaterialTheme.colors.primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false, // Allow multi-line (Shift+Enter)
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colors.primary else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colors.primary else Color(0xFF3E3E3E),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            elevation = 2.dp
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.Black else Color.White,
                style = MaterialTheme.typography.body1
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = if (isUser) "You" else "AI",
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}

@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFF3E3E3E),
            shape = RoundedCornerShape(16.dp),
            elevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colors.primary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "AI is thinking...",
                    color = Color.White,
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}
