package core.issues

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Issue types
 */
enum class IssueType {
    UNREGISTERED_LISTENER,
    MAIN_THREAD_SENSOR,
    OVER_SAMPLING,
    DUPLICATE_LISTENER,
    MISSING_PERMISSION,
    PRIVACY_LEAK
}

/**
 * Issue severity
 */
enum class Severity {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Represents a detected issue
 */
@Serializable
data class Issue(
    val id: String = UUID.randomUUID().toString(),
    val type: IssueType,
    val severity: Severity,
    val description: String,
    val nodeRefs: List<String> = emptyList(), // Node IDs
    val codeRefs: List<CodeReference> = emptyList(),
    val recommendation: String
)

@Serializable
data class CodeReference(
    val filePath: String,
    val lineNumber: Int
)
