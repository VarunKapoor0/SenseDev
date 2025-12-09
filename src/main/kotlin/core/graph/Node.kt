package core.graph

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Node types in the graph
 */
enum class NodeType {
    SENSOR_SOURCE,  // Entry point for sensor data
    LOGIC,          // Business logic / repository
    VIEWMODEL,      // ViewModel layer
    UI,             // UI component (Activity, Fragment, Composable)
    GENERIC         // Other nodes
}

/**
 * Sensor types
 */
enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    LIGHT,
    PROXIMITY,
    LOCATION,
    CAMERA,
    MICROPHONE,
    STEP_COUNTER,
    OTHER
}

/**
 * Thread hint for execution context
 */
enum class ThreadHint {
    MAIN,
    BACKGROUND,
    UNKNOWN
}

/**
 * Represents a node in the call/data flow graph
 */
@Serializable
data class Node(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: NodeType,
    val filePath: String,
    val methods: List<String> = emptyList(), // Method qualified names
    val inputs: List<String> = emptyList(),  // Node IDs
    val outputs: List<String> = emptyList(), // Node IDs
    val sensorTypes: List<SensorType> = emptyList(),
    val metadata: NodeMetadata = NodeMetadata()
)

@Serializable
data class NodeMetadata(
    val hasLifecycle: Boolean = false,
    val threadHint: ThreadHint = ThreadHint.UNKNOWN,
    val stateExposure: List<String> = emptyList() // LiveData, StateFlow, etc.
)
