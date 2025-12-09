package core.ai

data class AIPromptParts(
    val systemInstructions: String,
    val userQuestion: String,
    val graphContext: String,
    val codeSnippet: String?,
    val finalInstructions: String
)
