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
import ui.theme.*
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset

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
            .background(PanelBackground)
    ) {
        // Header
        Surface(
            color = PanelBackground,
            elevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
                .border(width = 0.dp, color = Color.Transparent) // No border for header
                .drawBehind { drawLine(BorderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }
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
            color = PanelBackground,
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
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
                        }
                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp)),
                    placeholder = { Text("Ask about this codebase...", color = Color(0xFF6F6F6F)) },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color(0xFF1F1F1F),
                        textColor = TextPrimary,
                        cursorColor = AccentPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = false, 
                    maxLines = 4
                )
                
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (inputText.isNotBlank()) AccentPrimary.copy(alpha=0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) AccentPrimary else Color.Gray
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
            color = if (isUser) Color(0xFF2D2D2D) else Color(0xFF222222),
            shape = RoundedCornerShape(8.dp),
            elevation = 1.dp,
            border = if (!isUser) BorderStroke(1.dp, Color(0xFF2C2C2C)) else null
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color(0xFFE0E0E0) else Color(0xFFCCCCCC),
                style = MaterialTheme.typography.body1.copy(
                    lineHeight = 22.sp
                )
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isUser) "You" else "SenseDev AI",
                style = MaterialTheme.typography.caption,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
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
