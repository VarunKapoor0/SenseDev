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

class GeminiProvider : AIProvider {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }
    
    /**
     * Query with structured parts (preferred for Gemini 2.x)
     */
    suspend fun queryWithParts(parts: AIPromptParts): String {
        if (!isConfigured()) {
            return "Gemini API key not configured. Please set it in Settings."
        }
        
        return try {
            val apiKey = AISettings.geminiApiKey
            val modelName = AISettings.selectedModel
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            
            // Build multi-part contents
            val contentParts = mutableListOf<Part>()
            contentParts.add(Part(text = parts.systemInstructions))
            contentParts.add(Part(text = parts.userQuestion))
            
            if (parts.codeSnippet != null) {
                contentParts.add(Part(text = "Relevant code:"))
                contentParts.add(Part(text = parts.codeSnippet))
            }
            
            contentParts.add(Part(text = parts.graphContext))
            contentParts.add(Part(text = parts.finalInstructions))
            
            val requestBody = GeminiRequest(
                contents = listOf(
                    Content(parts = contentParts)
                )
            )
            
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val geminiResponse = response.body<GeminiResponse>()
                geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "No response from Gemini"
            } else {
                val statusCode = response.status.value
                val statusDesc = response.status.description
                when (statusCode) {
                    400 -> "Invalid request format. Model may not support this query."
                    403 -> "API key does not have access to this model. Try a different model."
                    404 -> "Model '$modelName' not found. Check model name."
                    429 -> "Rate limit exceeded. Please wait and try again."
                    else -> "Gemini API error ($statusCode): $statusDesc"
                }
            }
        } catch (e: Exception) {
            "Gemini error: ${e.message}"
        }
    }
    
    /**
     * Legacy query method (single string)
     */
    override suspend fun query(prompt: String): String {
        if (!isConfigured()) {
            return "Gemini API key not configured. Please set it in Settings."
        }
        
        return try {
            val apiKey = AISettings.geminiApiKey
            val modelName = AISettings.selectedModel
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            
            val requestBody = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                )
            )
            
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            
            if (response.status.isSuccess()) {
                val geminiResponse = response.body<GeminiResponse>()
                geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "No response from Gemini"
            } else {
                val statusCode = response.status.value
                val statusDesc = response.status.description
                when (statusCode) {
                    400 -> "Invalid request format. Model may not support this query."
                    403 -> "API key does not have access to this model. Try a different model."
                    404 -> "Model '$modelName' not found. Check model name."
                    429 -> "Rate limit exceeded. Please wait and try again."
                    else -> "Gemini API error ($statusCode): $statusDesc"
                }
            }
        } catch (e: Exception) {
            "Gemini error: ${e.message}"
        }
    }
    
    override fun isConfigured(): Boolean {
        return AISettings.geminiApiKey.isNotBlank()
    }
    
    override fun getName(): String = "Gemini"
    
    override val capabilities = AIProviderCapabilities(
        maxContextTokens = 1000000, // Gemini 1.5/2.0 has huge context
        allowsMultipart = true,
        supportsKotlinCodeReasoning = true,
        supportsFileContext = true,
        responseFormats = listOf(LLMResponseFormat.TEXT, LLMResponseFormat.JSON, LLMResponseFormat.MARKDOWN)
    )
}

@Serializable
data class GeminiRequest(
    val contents: List<Content>
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)
