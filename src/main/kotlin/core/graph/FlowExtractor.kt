package core.graph

/**
 * Extracts data flows from sensor sources to UI
 */
class FlowExtractor {
    
    /**
     * Extract flows from the graph
     */
    fun extractFlows(graphData: GraphData): List<Flow> {
        val flows = mutableListOf<Flow>()
        
        // Find all sensor source nodes
        val sensorNodes = graphData.nodes.filter { it.type == NodeType.SENSOR_SOURCE }
        
        for (sensorNode in sensorNodes) {
            for (sensorType in sensorNode.sensorTypes) {
                // Trace paths from this sensor to UI nodes
                val paths = tracePaths(sensorNode, graphData)
                
                for (path in paths) {
                    flows.add(
                        Flow(
                            sensorType = sensorType,
                            path = path,
                            confidence = calculateConfidence(path, graphData)
                        )
                    )
                }
            }
        }
        
        return flows
    }
    
    private fun tracePaths(
        startNode: Node,
        graphData: GraphData,
        visited: Set<String> = emptySet(),
        currentPath: List<String> = emptyList()
    ): List<List<String>> {
        val paths = mutableListOf<List<String>>()
        val newPath = currentPath + startNode.id
        val newVisited = visited + startNode.id
        
        // If this is a UI node, we've found a complete path
        if (startNode.type == NodeType.UI) {
            paths.add(newPath)
            return paths
        }
        
        // Find outgoing edges from this node
        val outgoingEdges = graphData.edges.filter { it.from == startNode.id }
        
        for (edge in outgoingEdges) {
            // Only follow relevant edges for data flow
            // CALLS: Standard method call
            // WRITES_STATE: Writing to LiveData/StateFlow
            // USES_SENSOR_DATA: Explicit sensor usage passing
            if (edge.type == EdgeType.CALLS || 
                edge.type == EdgeType.WRITES_STATE || 
                edge.type == EdgeType.USES_SENSOR_DATA ||
                edge.type == EdgeType.DATA_FLOW) {
                
                if (edge.to !in newVisited) {
                    val nextNode = graphData.nodes.find { it.id == edge.to }
                    if (nextNode != null) {
                        val subPaths = tracePaths(nextNode, graphData, newVisited, newPath)
                        paths.addAll(subPaths)
                    }
                }
            }
        }
        
        return paths
    }
    
    private fun calculateConfidence(path: List<String>, graphData: GraphData): Float {
        // Simplified confidence based on path length
        // Shorter paths are more confident
        return when {
            path.size <= 2 -> 1.0f
            path.size == 3 -> 0.9f
            path.size == 4 -> 0.7f
            else -> 0.5f
        }
    }
}
