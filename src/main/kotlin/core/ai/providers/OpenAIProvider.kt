package core.ai.providers

import core.ai.AISettings
import core.ai.AIPromptParts
import core.ai.AIProviderCapabilities
import core.ai.LLMResponseFormat
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenAIProvider : AIProvider {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }
    
    /**
     * Query with structured parts (converts to OpenAI messages format)
     */
    suspend fun queryWithParts(parts: AIPromptParts): String {
        if (!isConfigured()) {
            return "OpenAI API key not configured. Please set it in Settings."
        }
        
        return try {
            val apiKey = AISettings.openaiApiKey
            val modelName = AISettings.selectedModel.ifBlank { "gpt-4" }
            val url = "https://api.openai.com/v1/chat/completions"
            
            // Build messages array
            val messages = mutableListOf<Message>()
            
            // System message
            messages.add(Message(
                role = "system",
                content = parts.systemInstructions
            ))
            
            // User message with all context
            val userContent = buildString {
                append(parts.userQuestion)
                append("\n\n")
                if (parts.codeSnippet != null) {
                    append("Relevant code:\n")
                    append(parts.codeSnippet)
                    append("\n\n")
                }
                append(parts.graphContext)
                append("\n\n")
                append(parts.finalInstructions)
            }
            
            messages.add(Message(
                role = "user",
                content = userContent
            ))
            
            val requestBody = OpenAIRequest(
                model = modelName,
                messages = messages,
                temperature = 0.7
            )
            
            val response: HttpResponse = client.post(url) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val openAIResponse = response.body<OpenAIResponse>()
                openAIResponse.choices?.firstOrNull()?.message?.content 
                    ?: "No response from OpenAI"
            } else {
                val statusCode = response.status.value
                val statusDesc = response.status.description
                when (statusCode) {
                    400 -> "Invalid request format. Check model name and parameters."
                    401 -> "Invalid API key. Please check your OpenAI API key in Settings."
                    403 -> "Access forbidden. Your API key may not have access to this model."
                    404 -> "Model '$modelName' not found. Try 'gpt-4' or 'gpt-3.5-turbo'."
                    429 -> "Rate limit exceeded or quota exhausted. Please wait and try again."
                    else -> "OpenAI API error ($statusCode): $statusDesc"
                }
            }
        } catch (e: Exception) {
            "OpenAI error: ${e.message}"
        }
    }
    
    /**
     * Legacy query method (single string)
     */
    override suspend fun query(prompt: String): String {
        if (!isConfigured()) {
            return "OpenAI API key not configured. Please set it in Settings."
        }
        
        return try {
            val apiKey = AISettings.openaiApiKey
            val modelName = AISettings.selectedModel.ifBlank { "gpt-4" }
            val url = "https://api.openai.com/v1/chat/completions"
            
            val requestBody = OpenAIRequest(
                model = modelName,
                messages = listOf(
                    Message(role = "user", content = prompt)
                ),
                temperature = 0.7
            )
            
            val response: HttpResponse = client.post(url) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val openAIResponse = response.body<OpenAIResponse>()
                openAIResponse.choices?.firstOrNull()?.message?.content 
                    ?: "No response from OpenAI"
            } else {
                val statusCode = response.status.value
                val statusDesc = response.status.description
                when (statusCode) {
                    400 -> "Invalid request format. Check model name and parameters."
                    401 -> "Invalid API key. Please check your OpenAI API key in Settings."
                    403 -> "Access forbidden. Your API key may not have access to this model."
                    404 -> "Model '$modelName' not found. Try 'gpt-4' or 'gpt-3.5-turbo'."
                    429 -> "Rate limit exceeded or quota exhausted. Please wait and try again."
                    else -> "OpenAI API error ($statusCode): $statusDesc"
                }
            }
        } catch (e: Exception) {
            "OpenAI error: ${e.message}"
        }
    }
    
    override fun isConfigured(): Boolean {
        return AISettings.openaiApiKey.isNotBlank()
    }
    
    override fun getName(): String = "OpenAI"
    
    override val capabilities = AIProviderCapabilities(
        maxContextTokens = 128000, // GPT-4 Turbo context window
        allowsMultipart = true,
        supportsKotlinCodeReasoning = true,
        supportsFileContext = true,
        responseFormats = listOf(LLMResponseFormat.TEXT, LLMResponseFormat.JSON, LLMResponseFormat.MARKDOWN)
    )
}

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.7
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class OpenAIResponse(
    val choices: List<Choice>? = null
)

@Serializable
data class Choice(
    val message: MessageResponse? = null
)

@Serializable
data class MessageResponse(
    val content: String? = null
)
