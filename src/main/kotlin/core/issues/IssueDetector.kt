package core.issues

import core.graph.GraphData
import core.graph.NodeType
import core.model.SymbolMap

/**
 * Detects common sensor-related issues
 */
class IssueDetector {
    
    /**
     * Detect issues in the analyzed code
     */
    fun detectIssues(
        symbolMap: SymbolMap,
        graphData: GraphData
    ): List<Issue> {
        val issues = mutableListOf<Issue>()
        
        // Rule 1: Unregistered sensor listeners
        issues.addAll(detectUnregisteredListeners(symbolMap))
        
        // Rule 2: Main thread sensor processing
        issues.addAll(detectMainThreadSensorProcessing(graphData))
        
        // Rule 3: Duplicate listeners
        issues.addAll(detectDuplicateListeners(symbolMap))
        
        // Rule 4: Privacy Leaks
        issues.addAll(detectPrivacyLeaks(graphData))
        
        return issues
    }
    
    private fun detectUnregisteredListeners(symbolMap: SymbolMap): List<Issue> {
        val issues = mutableListOf<Issue>()
        
        for (classSymbol in symbolMap.classes.values) {
            val hasRegister = classSymbol.methods.any { method ->
                method.callsTo.any { it.contains("registerListener") }
            }
            
            val hasUnregister = classSymbol.methods.any { method ->
                method.callsTo.any { it.contains("unregisterListener") }
            }
            
            if (hasRegister && !hasUnregister) {
                issues.add(
                    Issue(
                        type = IssueType.UNREGISTERED_LISTENER,
                        severity = Severity.HIGH,
                        description = "Sensor listener registered but never unregistered in ${classSymbol.name}",
                        codeRefs = listOf(
                            CodeReference(
                                filePath = classSymbol.filePath,
                                lineNumber = 0
                            )
                        ),
                        recommendation = "Add unregisterListener() call in onPause() or onDestroy() to avoid battery drain"
                    )
                )
            }
        }
        
        return issues
    }
    
    private fun detectMainThreadSensorProcessing(graphData: GraphData): List<Issue> {
        val issues = mutableListOf<Issue>()
        
        // Find sensor nodes that connect to UI without going through background processing
        val sensorNodes = graphData.nodes.filter { it.type == NodeType.SENSOR_SOURCE }
        val uiNodes = graphData.nodes.filter { it.type == NodeType.UI }
        
        for (sensorNode in sensorNodes) {
            for (uiNode in uiNodes) {
                val directEdge = graphData.edges.find {
                    it.from == sensorNode.id && it.to == uiNode.id
                }
                
                if (directEdge != null) {
                    issues.add(
                        Issue(
                            type = IssueType.MAIN_THREAD_SENSOR,
                            severity = Severity.MEDIUM,
                            description = "Sensor data processed directly on main thread in ${uiNode.name}",
                            nodeRefs = listOf(sensorNode.id, uiNode.id),
                            recommendation = "Move sensor data processing to a background thread or use coroutines"
                        )
                    )
                }
            }
        }
        
        return issues
    }
    
    private fun detectDuplicateListeners(symbolMap: SymbolMap): List<Issue> {
        val issues = mutableListOf<Issue>()
        
        for (classSymbol in symbolMap.classes.values) {
            val registerCalls = classSymbol.methods.flatMap { method ->
                method.callsTo.filter { it.contains("registerListener") }
            }
            
            if (registerCalls.size > 1) {
                val sensorTypes = registerCalls.groupBy { it }
                for ((call, occurrences) in sensorTypes) {
                    if (occurrences.size > 1) {
                        issues.add(
                            Issue(
                                type = IssueType.DUPLICATE_LISTENER,
                                severity = Severity.MEDIUM,
                                description = "Multiple sensor listener registrations in ${classSymbol.name}",
                                codeRefs = listOf(
                                    CodeReference(classSymbol.filePath, 0)
                                ),
                                recommendation = "Ensure sensor listener is only registered once"
                            )
                        )
                    }
                }
            }
        }
        
        return issues
    }

    private fun detectPrivacyLeaks(graphData: GraphData): List<Issue> {
        val issues = mutableListOf<Issue>()
        
        // Find paths from Sensor -> Network/Storage
        // This is a simplified check: look for nodes that are likely network/storage sinks
        // and check if they are reachable from sensor nodes
        
        val sensorNodes = graphData.nodes.filter { it.type == NodeType.SENSOR_SOURCE }
        
        // Identify potential sinks based on names or types (heuristic)
        val sinkNodes = graphData.nodes.filter { node ->
            val name = node.name.lowercase()
            name.contains("http") || 
            name.contains("retrofit") || 
            name.contains("database") || 
            name.contains("room") || 
            name.contains("sharedpreferences") ||
            name.contains("file")
        }
        
        for (sensorNode in sensorNodes) {
            for (sinkNode in sinkNodes) {
                // Check reachability (BFS/DFS)
                if (isReachable(sensorNode.id, sinkNode.id, graphData)) {
                    issues.add(
                        Issue(
                            type = IssueType.PRIVACY_LEAK,
                            severity = Severity.HIGH,
                            description = "Potential privacy leak: Sensor data from ${sensorNode.name} may flow to ${sinkNode.name}",
                            nodeRefs = listOf(sensorNode.id, sinkNode.id),
                            recommendation = "Ensure user consent is obtained before transmitting or storing sensor data"
                        )
                    )
                }
            }
        }
        
        return issues
    }
    
    private fun isReachable(startId: String, endId: String, graphData: GraphData): Boolean {
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(startId)
        visited.add(startId)
        
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            if (currentId == endId) return true
            
            val neighbors = graphData.edges
                .filter { it.from == currentId }
                .map { it.to }
            
            for (neighbor in neighbors) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        
        return false
    }
}
