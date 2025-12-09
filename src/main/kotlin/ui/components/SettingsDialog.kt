package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.window.Dialog
import core.ai.*
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit
) {
    var geminiKey by remember { mutableStateOf(AISettings.geminiApiKey) }
    var openaiKey by remember { mutableStateOf(AISettings.openaiApiKey) }
    var selectedProvider by remember { mutableStateOf(AISettings.selectedProvider) }
    // Separate model state for each provider
    var geminiModel by remember { mutableStateOf(
        if (AISettings.selectedProvider == AIProviderType.GEMINI) AISettings.selectedModel else ""
    ) }
    var openaiModel by remember { mutableStateOf(
        if (AISettings.selectedProvider == AIProviderType.OPENAI) AISettings.selectedModel else "gpt-4"
    ) }
    var codeAccessLevel by remember { mutableStateOf(AISettings.codeAccessLevel) }
    var maxTokens by remember { mutableStateOf(AISettings.maxCodeTokens.toString()) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenAIKey by remember { mutableStateOf(false) }
    
    // Model fetching
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFetchingModels by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    
    // Model testing
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val modelFetcher = remember { ModelFetcher() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF2D2D2D),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.width(600.dp).heightIn(max = 700.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())
            ) {
                Text(
                    "AI Provider Settings",
                    style = MaterialTheme.typography.h5,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Provider selection
                Text("AI Provider:", color = Color.Gray, style = MaterialTheme.typography.caption)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AIProviderType.values().forEach { provider ->
                        Button(
                            onClick = { selectedProvider = provider },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (selectedProvider == provider) {
                                    MaterialTheme.colors.primary
                                } else {
                                    Color(0xFF3E3E3E)
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(provider.name, color = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Provider-specific API Key
                when (selectedProvider) {
                    AIProviderType.GEMINI -> {
                        // Gemini API Key
                        Text("Gemini API Key:", color = Color.Gray, style = MaterialTheme.typography.caption)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = geminiKey,
                                onValueChange = { geminiKey = it },
                                modifier = Modifier.weight(1f),
                                visualTransformation = if (showGeminiKey) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    backgroundColor = Color(0xFF1E1E1E),
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = Color(0xFF3E3E3E)
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Text(if (showGeminiKey) "Hide" else "Show", color = MaterialTheme.colors.primary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Model Selection
                        Text("Model:", color = Color.Gray, style = MaterialTheme.typography.caption)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        var isModelDropdownExpanded by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = geminiModel,
                                    onValueChange = { geminiModel = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        textColor = Color.White,
                                        backgroundColor = Color(0xFF1E1E1E),
                                        focusedBorderColor = MaterialTheme.colors.primary,
                                        unfocusedBorderColor = Color(0xFF3E3E3E)
                                    ),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { isModelDropdownExpanded = !isModelDropdownExpanded }) {
                                            Icon(
                                                imageVector = if (isModelDropdownExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                contentDescription = "Select Model",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                )
                                
                                DropdownMenu(
                                    expanded = isModelDropdownExpanded,
                                    onDismissRequest = { isModelDropdownExpanded = false },
                                    modifier = Modifier.background(Color(0xFF2D2D2D))
                                ) {
                                    if (availableModels.isEmpty()) {
                                        DropdownMenuItem(onClick = { isModelDropdownExpanded = false }) {
                                            Text("No models fetched yet", color = Color.Gray)
                                        }
                                    } else {
                                        availableModels.forEach { model ->
                                            DropdownMenuItem(onClick = {
                                                geminiModel = model
                                                isModelDropdownExpanded = false
                                            }) {
                                                Text(model, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (geminiKey.isNotBlank()) {
                                        isFetchingModels = true
                                        fetchError = null
                                        coroutineScope.launch {
                                            try {
                                                availableModels = modelFetcher.fetchGeminiModels(geminiKey)
                                                if (availableModels.isNotEmpty()) {
                                                    geminiModel = availableModels.first()
                                                    isModelDropdownExpanded = true // Auto-expand to show options
                                                }
                                                isFetchingModels = false
                                            } catch (e: Exception) {
                                                fetchError = e.message
                                                isFetchingModels = false
                                            }
                                        }
                                    }
                                },
                                enabled = geminiKey.isNotBlank() && !isFetchingModels
                            ) {
                                Text(if (isFetchingModels) "Loading..." else "Fetch Models")
                            }
                        }
                        
                        if (fetchError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Error: $fetchError", color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Test Model Button
                        Button(
                            onClick = {
                                if (geminiKey.isNotBlank()) {
                                    isTesting = true
                                    coroutineScope.launch {
                                        val result = modelFetcher.testModel(geminiKey, geminiModel)
                                        testResult = if (result.success) {
                                            "✓ ${result.message}"
                                        } else {
                                            "✗ ${result.message}"
                                        }
                                        isTesting = false
                                    }
                                }
                            },
                            enabled = geminiKey.isNotBlank() && !isTesting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isTesting) "Testing..." else "Test Model Connection")
                        }
                        
                        if (testResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                testResult!!,
                                color = if (testResult!!.startsWith("✓")) Color(0xFF81C784) else MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                    
                    AIProviderType.OPENAI -> {
                        // OpenAI API Key
                        Text("OpenAI API Key:", color = Color.Gray, style = MaterialTheme.typography.caption)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = openaiKey,
                                onValueChange = { openaiKey = it },
                                modifier = Modifier.weight(1f),
                                visualTransformation = if (showOpenAIKey) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    backgroundColor = Color(0xFF1E1E1E),
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = Color(0xFF3E3E3E)
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { showOpenAIKey = !showOpenAIKey }) {
                                Text(if (showOpenAIKey) "Hide" else "Show", color = MaterialTheme.colors.primary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Model Selection (manual for OpenAI)
                        Text("Model:", color = Color.Gray, style = MaterialTheme.typography.caption)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        var isOpenAIModelDropdownExpanded by remember { mutableStateOf(false) }
                        val openAIModels = listOf("gpt-4", "gpt-4-turbo", "gpt-3.5-turbo", "gpt-4o")
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = openaiModel,
                                onValueChange = { openaiModel = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color.White,
                                    backgroundColor = Color(0xFF1E1E1E),
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = Color(0xFF3E3E3E)
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { isOpenAIModelDropdownExpanded = !isOpenAIModelDropdownExpanded }) {
                                        Icon(
                                            imageVector = if (isOpenAIModelDropdownExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Select Model",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            )
                            
                            DropdownMenu(
                                expanded = isOpenAIModelDropdownExpanded,
                                onDismissRequest = { isOpenAIModelDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF2D2D2D))
                            ) {
                                openAIModels.forEach { model ->
                                    DropdownMenuItem(onClick = {
                                        openaiModel = model
                                        isOpenAIModelDropdownExpanded = false
                                    }) {
                                        Text(model, color = Color.White)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Note: Enter your OpenAI API key to use GPT models",
                            color = Color.Gray,
                            style = MaterialTheme.typography.caption
                        )
                    }
                    
                    AIProviderType.LOCAL -> {
                        Text(
                            "Local AI uses built-in pattern matching. No API key required.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.body2
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = Color(0xFF3E3E3E))
                Spacer(modifier = Modifier.height(24.dp))
                
                // Code Access Level
                Text("Code Access Level:", color = Color.Gray, style = MaterialTheme.typography.caption)
                Spacer(modifier = Modifier.height(8.dp))
                
                CodeAccessLevel.values().forEach { level ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = codeAccessLevel == level,
                            onClick = { codeAccessLevel = level },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colors.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                when (level) {
                                    CodeAccessLevel.SNIPPET_ONLY -> "Snippet-only (safe default)"
                                    CodeAccessLevel.FLOW_LEVEL -> "Flow-level context (multi-file)"
                                    CodeAccessLevel.FULL_FILE -> "Full file access (explicit opt-in)"
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.body2
                            )
                            Text(
                                when (level) {
                                    CodeAccessLevel.SNIPPET_ONLY -> "Only selected method/class"
                                    CodeAccessLevel.FLOW_LEVEL -> "Include graph-adjacent nodes"
                                    CodeAccessLevel.FULL_FILE -> "Entire files sent to AI"
                                },
                                color = Color.Gray,
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Max Code Tokens
                Text("Maximum Code Tokens:", color = Color.Gray, style = MaterialTheme.typography.caption)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            maxTokens = newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        backgroundColor = Color(0xFF1E1E1E),
                        focusedBorderColor = MaterialTheme.colors.primary,
                        unfocusedBorderColor = Color(0xFF3E3E3E)
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        // Save settings
                        AISettings.geminiApiKey = geminiKey
                        AISettings.openaiApiKey = openaiKey
                        AISettings.selectedProvider = selectedProvider
                        // Save the appropriate model based on selected provider
                        AISettings.selectedModel = when (selectedProvider) {
                            AIProviderType.GEMINI -> geminiModel
                            AIProviderType.OPENAI -> openaiModel
                            else -> ""
                        }
                        AISettings.codeAccessLevel = codeAccessLevel
                        AISettings.maxCodeTokens = maxTokens.toIntOrNull() ?: 2000
                        AISettings.saveSettings()
                        onSettingsChanged()
                        onDismiss()
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
