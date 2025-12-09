package core.ai

import kotlinx.serialization.Serializable
import java.util.UUID

enum class MessageRole {
    USER,
    AI,
    SYSTEM
}

enum class AIProviderType {
    LOCAL,
    GEMINI,
    OPENAI
}

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: AIProviderType = AIProviderType.LOCAL
)

data class AIContext(
    val selectedNodeId: String? = null,
    val selectedCodeFile: String? = null,
    val selectedCodeLine: Int? = null,
    val graphData: core.graph.GraphData? = null,
    val projectPath: String? = null
)
