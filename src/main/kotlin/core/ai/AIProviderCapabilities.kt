package core.ai

enum class LLMResponseFormat {
    TEXT,
    JSON,
    MARKDOWN
}

data class AIProviderCapabilities(
    val maxContextTokens: Int,
    val allowsMultipart: Boolean,
    val supportsKotlinCodeReasoning: Boolean,
    val supportsFileContext: Boolean,
    val responseFormats: List<LLMResponseFormat>
)
