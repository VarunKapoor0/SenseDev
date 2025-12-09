package core.sensor

import core.graph.SensorType
import core.model.ClassSymbol
import core.model.MethodSymbol

/**
 * Detects sensor-related API usage in parsed code
 */
class SensorDetector {
    
    data class SensorEntryPoint(
        val classQualifiedName: String,
        val methodQualifiedName: String,
        val sensorType: SensorType,
        val filePath: String,
        val lineNumber: Int
    )
    
    /**
     * Detect all sensor entry points in the given classes
     */
    fun detectSensorUsage(classes: List<ClassSymbol>): List<SensorEntryPoint> {
        val entryPoints = mutableListOf<SensorEntryPoint>()
        
        for (classSymbol in classes) {
            for (method in classSymbol.methods) {
                val sensors = detectSensorsInMethod(method, classSymbol)
                entryPoints.addAll(sensors)
            }
        }
        
        return entryPoints
    }
    
    private fun detectSensorsInMethod(
        method: MethodSymbol,
        classSymbol: ClassSymbol
    ): List<SensorEntryPoint> {
        val entryPoints = mutableListOf<SensorEntryPoint>()
        
        // Check method name patterns
        when {
            method.name == "onSensorChanged" -> {
                entryPoints.add(
                    SensorEntryPoint(
                        classQualifiedName = classSymbol.qualifiedName,
                        methodQualifiedName = method.qualifiedName,
                        sensorType = SensorType.OTHER, // Will be refined based on registration
                        filePath = classSymbol.filePath,
                        lineNumber = method.lineNumber
                    )
                )
            }
            method.name.contains("onLocationChanged", ignoreCase = true) -> {
                entryPoints.add(
                    SensorEntryPoint(
                        classQualifiedName = classSymbol.qualifiedName,
                        methodQualifiedName = method.qualifiedName,
                        sensorType = SensorType.LOCATION,
                        filePath = classSymbol.filePath,
                        lineNumber = method.lineNumber
                    )
                )
            }
            method.name.contains("onCameraFrame", ignoreCase = true) ||
            method.name.contains("onPreviewFrame", ignoreCase = true) -> {
                entryPoints.add(
                    SensorEntryPoint(
                        classQualifiedName = classSymbol.qualifiedName,
                        methodQualifiedName = method.qualifiedName,
                        sensorType = SensorType.CAMERA,
                        filePath = classSymbol.filePath,
                        lineNumber = method.lineNumber
                    )
                )
            }
        }
        
        // Check for sensor registration patterns in method calls
        for (call in method.callsTo) {
            when {
                call.contains("registerListener") && call.contains("Sensor") -> {
                    val sensorType = inferSensorTypeFromCall(call)
                    entryPoints.add(
                        SensorEntryPoint(
                            classQualifiedName = classSymbol.qualifiedName,
                            methodQualifiedName = method.qualifiedName,
                            sensorType = sensorType,
                            filePath = classSymbol.filePath,
                            lineNumber = method.lineNumber
                        )
                    )
                }
                call.contains("AudioRecord") -> {
                    entryPoints.add(
                        SensorEntryPoint(
                            classQualifiedName = classSymbol.qualifiedName,
                            methodQualifiedName = method.qualifiedName,
                            sensorType = SensorType.MICROPHONE,
                            filePath = classSymbol.filePath,
                            lineNumber = method.lineNumber
                        )
                    )
                }
            }
        }
        
        return entryPoints
    }
    
    private fun inferSensorTypeFromCall(call: String): SensorType {
        return when {
            call.contains("ACCELEROMETER", ignoreCase = true) -> SensorType.ACCELEROMETER
            call.contains("GYROSCOPE", ignoreCase = true) -> SensorType.GYROSCOPE
            call.contains("LIGHT", ignoreCase = true) -> SensorType.LIGHT
            call.contains("PROXIMITY", ignoreCase = true) -> SensorType.PROXIMITY
            call.contains("MAGNETIC", ignoreCase = true) -> SensorType.MAGNETOMETER
            call.contains("STEP", ignoreCase = true) -> SensorType.STEP_COUNTER
            else -> SensorType.OTHER
        }
    }
}
