package core.ai

import core.ai.providers.GeminiProvider
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

class ModelFetcher {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }
    
    /**
     * Fetch available Gemini models from API
     */
    suspend fun fetchGeminiModels(apiKey: String): List<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            
            val response: HttpResponse = client.get(url)
            
            if (response.status.isSuccess()) {
                val modelsResponse = response.body<GeminiModelsResponse>()
                
                // Filter to supported models (those with generateContent method)
                modelsResponse.models
                    ?.filter { model ->
                        model.supportedGenerationMethods?.contains("generateContent") == true
                    }
                    ?.map { it.name.removePrefix("models/") }
                    ?: emptyList()
            } else {
                throw Exception("Failed to fetch models: ${response.status.description}")
            }
        } catch (e: Exception) {
            throw Exception("Model fetch error: ${e.message}")
        }
    }
    
    /**
     * Test model connectivity with a simple query
     */
    suspend fun testModel(apiKey: String, modelName: String): TestResult {
        return try {
            val provider = GeminiProvider()
            // Simple test query
            val response = provider.query("Hello from SenseDev")
            
            if (response.contains("error", ignoreCase = true)) {
                TestResult(false, "Model returned error: $response")
            } else {
                TestResult(true, "Model responded successfully: ${response.take(150)}")
            }
        } catch (e: Exception) {
            TestResult(false, "Test failed: ${e.message}")
        }
    }
}

data class TestResult(
    val success: Boolean,
    val message: String
)

@Serializable
data class GeminiModelsResponse(
    val models: List<GeminiModelInfo>? = null
)

@Serializable
data class GeminiModelInfo(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null
)
