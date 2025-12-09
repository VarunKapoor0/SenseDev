package core.ai.providers

import core.ai.AIProviderCapabilities

interface AIProvider {
    suspend fun query(prompt: String): String
    fun isConfigured(): Boolean
    fun getName(): String
    
    val capabilities: AIProviderCapabilities
}
