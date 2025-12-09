package core.graph

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Edge types in the graph
 */
enum class EdgeType {
    CALLS,              // Method calls another method
    READS_STATE,        // Reads from LiveData/StateFlow
    WRITES_STATE,       // Writes to LiveData/StateFlow
    USES_SENSOR_DATA,   // Uses sensor data
    LIFECYCLE_LINK,     // Lifecycle relationship
    DATA_FLOW           // Implicit data flow (e.g. callback/subscription)
}

/**
 * Represents an edge (relationship) between two nodes
 */
@Serializable
data class Edge(
    val id: String = UUID.randomUUID().toString(),
    val from: String, // Node ID
    val to: String,   // Node ID
    val type: EdgeType,
    val sourceLocation: SourceLocation? = null
)

@Serializable
data class SourceLocation(
    val filePath: String,
    val lineNumber: Int
)
