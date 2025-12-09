package core.graph

import core.model.ClassSymbol
import core.model.SymbolMap
import core.sensor.SensorDetector
import kotlinx.serialization.Serializable

/**
 * Builds the call graph and data flow graph from parsed symbols
 */
class GraphBuilder {
    
    /**
     * Build graph from symbol map and sensor entry points
     */
    fun buildGraph(
        symbolMap: SymbolMap,
        sensorEntryPoints: List<SensorDetector.SensorEntryPoint>
    ): GraphData {
        val nodes = mutableMapOf<String, Node>()
        val edges = mutableListOf<Edge>()
        
        // Create nodes for each class
        for ((qualifiedName, classSymbol) in symbolMap.classes) {
            val nodeType = determineNodeType(classSymbol, sensorEntryPoints)
            val sensorTypes = getSensorTypesForClass(classSymbol, sensorEntryPoints)
            
            val node = Node(
                name = classSymbol.name,
                type = nodeType,
                filePath = classSymbol.filePath,
                methods = classSymbol.methods.map { it.qualifiedName },
                sensorTypes = sensorTypes,
                metadata = NodeMetadata(
                    hasLifecycle = classSymbol.isActivity || classSymbol.isFragment,
                    threadHint = ThreadHint.UNKNOWN,
                    stateExposure = classSymbol.fields
                        .filter { it.isLiveData || it.isStateFlow || it.isFlow }
                        .map { it.type }
                )
            )
            
            nodes[qualifiedName] = node
        }
        
        // Create edges based on method calls and field access
        for ((qualifiedName, classSymbol) in symbolMap.classes) {
            val fromNode = nodes[qualifiedName] ?: continue
            
            // Create edges based on method calls and field access
            for (method in classSymbol.methods) {
                for (calledMethod in method.callsTo) {
                    // Try to resolve which class this method belongs to
                    val toClass = findClassByMethodCall(calledMethod, symbolMap, classSymbol)
                    if (toClass != null) {
                        val toNode = nodes[toClass.qualifiedName]
                        if (toNode != null && toNode.id != fromNode.id) {
                            val edgeType = determineEdgeType(method, calledMethod, classSymbol, toClass)
                            
                            edges.add(
                                Edge(
                                    from = fromNode.id,
                                    to = toNode.id,
                                    type = edgeType,
                                    sourceLocation = SourceLocation(
                                        filePath = classSymbol.filePath,
                                        lineNumber = method.lineNumber
                                    )
                                )
                            )
                            
                            // If this is a subscription/observation, add a reverse DATA_FLOW edge
                            if (edgeType == EdgeType.READS_STATE || edgeType == EdgeType.USES_SENSOR_DATA) {
                                edges.add(
                                    Edge(
                                        from = toNode.id,
                                        to = fromNode.id,
                                        type = EdgeType.DATA_FLOW
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Create edges for state flow (LiveData/StateFlow)
            for (field in classSymbol.fields) {
                if (field.isLiveData || field.isStateFlow || field.isFlow) {
                    // Classes that observe this state
                    val observers = findObservers(classSymbol, symbolMap)
                    for (observer in observers) {
                        val toNode = nodes[observer.qualifiedName]
                        if (toNode != null) {
                            edges.add(
                                Edge(
                                    from = fromNode.id,
                                    to = toNode.id,
                                    type = EdgeType.WRITES_STATE
                                )
                            )
                        }
                    }
                }
            }
        }
        
        return GraphData(nodes.values.toList(), edges)
    }
    
    private fun determineNodeType(
        classSymbol: ClassSymbol,
        sensorEntryPoints: List<SensorDetector.SensorEntryPoint>
    ): NodeType {
        // Check if this class has sensor entry points
        val hasSensorEntry = sensorEntryPoints.any { it.classQualifiedName == classSymbol.qualifiedName }
        if (hasSensorEntry) return NodeType.SENSOR_SOURCE
        
        // Check based on inheritance
        return when {
            classSymbol.isViewModel -> NodeType.VIEWMODEL
            classSymbol.isActivity || classSymbol.isFragment || classSymbol.isComposable -> NodeType.UI
            classSymbol.name.contains("Repository", ignoreCase = true) ||
            classSymbol.name.contains("Manager", ignoreCase = true) -> NodeType.LOGIC
            else -> NodeType.GENERIC
        }
    }
    
    private fun getSensorTypesForClass(
        classSymbol: ClassSymbol,
        sensorEntryPoints: List<SensorDetector.SensorEntryPoint>
    ): List<SensorType> {
        return sensorEntryPoints
            .filter { it.classQualifiedName == classSymbol.qualifiedName }
            .map { it.sensorType }
            .distinct()
    }
    
    private fun findClassByMethod(methodQualifiedName: String, symbolMap: SymbolMap): ClassSymbol? {
        for (classSymbol in symbolMap.classes.values) {
            if (classSymbol.methods.any { it.qualifiedName == methodQualifiedName }) {
                return classSymbol
            }
        }
        return null
    }
    
    private fun findObservers(classSymbol: ClassSymbol, symbolMap: SymbolMap): List<ClassSymbol> {
        // Simplified: assume UI and ViewModels observe state
        return symbolMap.classes.values.filter {
            it.isActivity || it.isFragment || it.isComposable || it.isViewModel
        }
    }
    
    private fun findClassByMethodCall(methodCall: String, symbolMap: SymbolMap): ClassSymbol? {
        // If it's a direct call "MethodName", look for it in the project
        if (!methodCall.contains(".")) {
            for (classSymbol in symbolMap.classes.values) {
                if (classSymbol.methods.any { it.name == methodCall }) {
                    return classSymbol
                }
            }
            return null
        }
        
        // If it's "Receiver.Method", try to resolve Receiver
        val parts = methodCall.split(".")
        if (parts.size >= 2) {
            val receiverName = parts[0]
            val methodName = parts[1]
            
            // Case 1: Receiver is a class name (Static call or Object)
            val receiverClass = symbolMap.classes[receiverName] ?: 
                                symbolMap.classes.values.find { it.name == receiverName }
            
            if (receiverClass != null) {
                return receiverClass
            }
            
            // Case 2: Receiver is a variable/field
            // We need to find the type of this variable in the current context
            // This is hard without full scope analysis, but we can check fields of the *calling* class
            // Note: We need 'fromClass' context here, but this method signature doesn't have it.
            // For now, we'll try to find ANY class that has a method with this name
            // This is a "loose" resolution strategy for MVP
             for (classSymbol in symbolMap.classes.values) {
                if (classSymbol.methods.any { it.name == methodName }) {
                    return classSymbol
                }
            }
        }
        
        return null
    }
    
    // Overloaded version with context
    private fun findClassByMethodCall(
        methodCall: String, 
        symbolMap: SymbolMap, 
        fromClass: ClassSymbol
    ): ClassSymbol? {
        if (!methodCall.contains(".")) {
             // Implicit 'this' or static import
             // Check if it's in the same class
             if (fromClass.methods.any { it.name == methodCall }) {
                 return fromClass
             }
             // Check other classes (loose resolution)
             return findClassByMethodCall(methodCall, symbolMap)
        }
        
        val parts = methodCall.split(".")
        val receiverName = parts[0]
        
        // Try to resolve receiver as a field in fromClass
        val field = fromClass.fields.find { it.name == receiverName }
        if (field != null) {
            // Found field, try to find class by type
            // Type might be "Repository" or "MyViewModel"
            val fieldType = field.type.replace("?", "").trim() // Handle nullable
            
            // Try exact match
            var typeClass = symbolMap.classes[fieldType] ?: 
                            symbolMap.classes.values.find { it.name == fieldType }
            
            if (typeClass != null) return typeClass
            
            // Try generic match (e.g. LiveData<Type>)
            if (fieldType.contains("<")) {
                val genericType = fieldType.substringAfter("<").substringBefore(">")
                typeClass = symbolMap.classes[genericType] ?: 
                            symbolMap.classes.values.find { it.name == genericType }
                if (typeClass != null) return typeClass
            }
        }
        
        // Fallback to simple resolution
        return findClassByMethodCall(methodCall, symbolMap)
    }
    
    private fun determineEdgeType(
        fromMethod: core.model.MethodSymbol,
        toMethodCall: String,
        fromClass: ClassSymbol,
        toClass: ClassSymbol
    ): EdgeType {
        val methodName = if (toMethodCall.contains(".")) {
            toMethodCall.split(".")[1]
        } else {
            toMethodCall
        }
        
        return when {
            // Sensor usage
            methodName.contains("registerListener", ignoreCase = true) -> EdgeType.USES_SENSOR_DATA
            methodName.contains("Listening", ignoreCase = true) -> EdgeType.USES_SENSOR_DATA
            toClass.name.contains("Sensor", ignoreCase = true) && 
                (methodName.contains("get") || methodName.contains("read")) -> EdgeType.USES_SENSOR_DATA
                
            // State writes
            methodName.startsWith("set") -> EdgeType.WRITES_STATE
            methodName.contains("update") -> EdgeType.WRITES_STATE
            methodName == "postValue" || methodName == "setValue" -> EdgeType.WRITES_STATE
            methodName == "emit" || methodName == "tryEmit" -> EdgeType.WRITES_STATE
            
            // State reads
            methodName.startsWith("get") -> EdgeType.READS_STATE
            methodName.contains("observe") -> EdgeType.READS_STATE
            methodName.contains("collect") -> EdgeType.READS_STATE
            
            else -> EdgeType.CALLS
        }
    }
}

/**
 * Graph data container
 */
@Serializable
data class GraphData(
    val nodes: List<Node>,
    val edges: List<Edge>
)
