package core.graph

import kotlinx.serialization.Serializable

/**
 * Represents a data flow from sensor to UI
 */
@Serializable
data class Flow(
    val sensorType: SensorType,
    val path: List<String>, // List of Node IDs
    val confidence: Float = 1.0f // 0.0 to 1.0
)
