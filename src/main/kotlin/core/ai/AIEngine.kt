package core.ai

import core.ai.providers.AIProvider
import core.ai.providers.GeminiProvider
import core.ai.providers.OpenAIProvider

class AIEngine {
    private val localEngine = LocalExplanationEngine()
    private val contextAssembler = ContextAssembler()
    private val geminiProvider = GeminiProvider()
    private val openaiProvider = OpenAIProvider()

    suspend fun processQuery(query: String, context: AIContext): ChatMessage {
        // Determine which provider to use
        val useExternal = AISettings.selectedProvider != AIProviderType.LOCAL
        
        val (responseContent, provider) = if (useExternal) {
            // Try external provider
            val externalProvider = getExternalProvider()
            if (externalProvider != null && externalProvider.isConfigured()) {
                try {
                    // Use multi-part format for Gemini and OpenAI
                    val response = if (externalProvider is GeminiProvider) {
                        val promptParts = contextAssembler.buildPromptParts(query, context, context.projectPath)
                        externalProvider.queryWithParts(promptParts)
                    } else if (externalProvider is OpenAIProvider) {
                        val promptParts = contextAssembler.buildPromptParts(query, context, context.projectPath)
                        externalProvider.queryWithParts(promptParts)
                    } else {
                        // Fallback to single-string for other providers
                        val prompt = contextAssembler.buildPrompt(query, context, context.projectPath)
                        externalProvider.query(prompt)
                    }
                    response to getProviderType(externalProvider)
                } catch (e: Exception) {
                    // Fallback to local on error
                    println("External provider failed, falling back to local: ${e.message}")
                    localEngine.explain(query, context) to AIProviderType.LOCAL
                }
            } else {
                // No provider configured, use local
                localEngine.explain(query, context) to AIProviderType.LOCAL
            }
        } else {
            // Use local by default
            localEngine.explain(query, context) to AIProviderType.LOCAL
        }
        
        return ChatMessage(
            role = MessageRole.AI,
            content = responseContent,
            provider = provider
        )
    }
    
    private fun getExternalProvider(): AIProvider? {
        return when (AISettings.selectedProvider) {
            AIProviderType.GEMINI -> geminiProvider
            AIProviderType.OPENAI -> openaiProvider
            else -> null
        }
    }
    
    private fun getProviderType(provider: AIProvider): AIProviderType {
        return when (provider) {
            is GeminiProvider -> AIProviderType.GEMINI
            is OpenAIProvider -> AIProviderType.OPENAI
            else -> AIProviderType.LOCAL
        }
    }
}
