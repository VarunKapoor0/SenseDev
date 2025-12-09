package core.ai

import core.graph.GraphData
import core.graph.Node
import core.graph.NodeType

class LocalExplanationEngine {

    fun explain(query: String, context: AIContext): String {
        val graph = context.graphData ?: return "No analysis data available to answer your question."
        val lowerQuery = query.lowercase()

        // 1. Check for specific node questions
        val mentionedNode = graph.nodes.find { lowerQuery.contains(it.name.lowercase()) }
        if (mentionedNode != null) {
            return explainNode(mentionedNode, graph)
        }

        // 2. Check for flow questions
        if (lowerQuery.contains("flow")) {
            return explainFlows(graph)
        }

        // 3. Check for issues
        if (lowerQuery.contains("issue") || lowerQuery.contains("problem") || lowerQuery.contains("bug")) {
            return explainIssues(graph) // Placeholder, issues are not in GraphData directly yet, usually in AnalysisResult
        }
        
        // 4. Context-aware fallback
        if (context.selectedNodeId != null && (lowerQuery.contains("this") || lowerQuery.contains("selected"))) {
            val selectedNode = graph.nodes.find { it.id == context.selectedNodeId }
            if (selectedNode != null) {
                return explainNode(selectedNode, graph)
            }
        }

        return "I am the Basic AI. I can explain nodes and flows found in the static analysis. Try asking about a specific class name or 'show flows'."
    }

    private fun explainNode(node: Node, graph: GraphData): String {
        val incoming = graph.edges.filter { it.to == node.id }
        val outgoing = graph.edges.filter { it.from == node.id }

        val sb = StringBuilder()
        sb.append("**${node.name}** is a **${node.type}**.\n\n")
        
        if (node.filePath.isNotEmpty()) {
            sb.append("File: `${node.filePath}`\n\n")
        }

        if (incoming.isNotEmpty()) {
            sb.append("**Referenced by (${incoming.size}):**\n")
            incoming.forEach { edge ->
                val fromNode = graph.nodes.find { it.id == edge.from }
                if (fromNode != null) {
                    sb.append("- ${fromNode.name} (${edge.type})\n")
                }
            }
            sb.append("\n")
        }

        if (outgoing.isNotEmpty()) {
            sb.append("**Calls/Uses (${outgoing.size}):**\n")
            outgoing.forEach { edge ->
                val toNode = graph.nodes.find { it.id == edge.to }
                if (toNode != null) {
                    sb.append("- ${toNode.name} (${edge.type})\n")
                }
            }
        }

        return sb.toString()
    }

    private fun explainFlows(graph: GraphData): String {
        val sensorNodes = graph.nodes.filter { it.type == NodeType.SENSOR_SOURCE }
        if (sensorNodes.isEmpty()) {
            return "No sensor sources detected in this project."
        }

        val sb = StringBuilder()
        sb.append("Found **${sensorNodes.size}** sensor sources:\n\n")

        sensorNodes.forEach { sensor ->
            sb.append("- **${sensor.name}**\n")
            // Simple DFS to find UI sinks
            val sinks = findSinks(sensor.id, graph)
            if (sinks.isNotEmpty()) {
                sb.append("  - Flows to: ${sinks.joinToString(", ") { it.name }}\n")
            } else {
                sb.append("  - No direct path to UI detected.\n")
            }
        }

        return sb.toString()
    }
    
    private fun explainIssues(graph: GraphData): String {
        // Since issues are not in GraphData, we might need to pass them in context or just give a generic message
        return "Please check the 'Issues' tab to see detected problems. I can explain specific nodes involved in those issues if you ask about them."
    }

    private fun findSinks(startId: String, graph: GraphData): List<Node> {
        val sinks = mutableListOf<Node>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(startId)
        visited.add(startId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val currentNode = graph.nodes.find { it.id == currentId }
            
            if (currentNode != null && currentNode.type == NodeType.UI) {
                sinks.add(currentNode)
            }

            val neighbors = graph.edges.filter { it.from == currentId }.map { it.to }
            for (neighbor in neighbors) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        return sinks
    }
}
