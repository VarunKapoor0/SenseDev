package core.ai

import core.graph.GraphData
import core.graph.Node

class ContextAssembler {
    
    private val codeExtractor = CodeExtractor()
    
    /**
     * Build structured prompt parts for multi-part API format (e.g., Gemini 2.x)
     */
    fun buildPromptParts(query: String, context: AIContext, projectPath: String? = null): AIPromptParts {
        val graph = context.graphData
        
        // 1. System instructions
        val systemInstructions = """
You are an AI assistant integrated into SenseDev, a static analysis tool for Android sensor usage.

CRITICAL INSTRUCTIONS:
- Only answer using the provided facts below
- If something is unknown, say 'This cannot be determined from static analysis'
- Do not invent files, variables, or code that isn't shown
- Be concise, technical, and helpful
        """.trimIndent()
        
        // 2. User question
        val userQuestion = "User Question: $query"
        
        // 3. Code snippet (if available)
        var codeSnippet: String? = null
        if (graph != null && context.selectedNodeId != null && projectPath != null) {
            val accessLevel = AISettings.codeAccessLevel
            val maxTokens = AISettings.maxCodeTokens
            
            val extractedCode = codeExtractor.extractCode(
                context.selectedNodeId,
                graph,
                projectPath,
                accessLevel,
                maxTokens
            )
            
            if (extractedCode.isNotBlank()) {
                codeSnippet = """
=== SOURCE CODE ===
(Access Level: ${accessLevel.name})

$extractedCode
                """.trimIndent()
            }
        }
        
        // 4. Graph context
        val graphContext = buildGraphContext(graph, context.selectedNodeId)
        
        // 5. Final instructions
        val finalInstructions = """
IMPORTANT: Reference ONLY the provided code and graph context above.
If information is missing, explicitly state what cannot be determined.
        """.trimIndent()
        
        return AIPromptParts(
            systemInstructions = systemInstructions,
            userQuestion = userQuestion,
            graphContext = graphContext,
            codeSnippet = codeSnippet,
            finalInstructions = finalInstructions
        )
    }
    
    /**
     * Legacy method - builds single-string prompt
     */
    fun buildPrompt(query: String, context: AIContext, projectPath: String? = null): String {
        val sb = StringBuilder()
        
        // System instructions
        sb.append("You are an AI assistant integrated into SenseDev, a static analysis tool for Android sensor usage.\n\n")
        sb.append("CRITICAL INSTRUCTIONS:\n")
        sb.append("- Only answer using the provided facts below\n")
        sb.append("- If something is unknown, say 'This cannot be determined from static analysis'\n")
        sb.append("- Do not invent files, variables, or code that isn't shown\n")
        sb.append("- Be concise, technical, and helpful\n\n")
        
        // Add code snippets if available
        val graph = context.graphData
        if (graph != null && context.selectedNodeId != null && projectPath != null) {
            val accessLevel = AISettings.codeAccessLevel
            val maxTokens = AISettings.maxCodeTokens
            
            val codeSnippet = codeExtractor.extractCode(
                context.selectedNodeId,
                graph,
                projectPath,
                accessLevel,
                maxTokens
            )
            
            if (codeSnippet.isNotBlank()) {
                sb.append("=== SOURCE CODE ===\n")
                sb.append("(Access Level: ${accessLevel.name})\n\n")
                sb.append(codeSnippet)
                sb.append("\n\n")
            }
        }
        
        // Add graph context
        if (graph != null) {
            sb.append("=== CODEBASE FACTS ===\n\n")
            
            // Selected node context
            if (context.selectedNodeId != null) {
                val selectedNode = graph.nodes.find { it.id == context.selectedNodeId }
                if (selectedNode != null) {
                    sb.append("SELECTED NODE:\n")
                    sb.append(nodeToText(selectedNode, graph))
                    sb.append("\n")
                }
            }
            
            // Related nodes (limit to 12)
            val relatedNodes = getRelatedNodes(context.selectedNodeId, graph).take(12)
            if (relatedNodes.isNotEmpty()) {
                sb.append("RELATED NODES:\n")
                relatedNodes.forEach { node ->
                    sb.append("- ${node.name} (${node.type})\n")
                }
                sb.append("\n")
            }
            
            // Edges (limit to 20)
            val edges = if (context.selectedNodeId != null) {
                graph.edges.filter { 
                    it.from == context.selectedNodeId || it.to == context.selectedNodeId 
                }.take(20)
            } else {
                graph.edges.take(20)
            }
            
            if (edges.isNotEmpty()) {
                sb.append("CONNECTIONS:\n")
                edges.forEach { edge ->
                    val fromNode = graph.nodes.find { it.id == edge.from }
                    val toNode = graph.nodes.find { it.id == edge.to }
                    sb.append("- ${fromNode?.name ?: "Unknown"} --[${edge.type}]--> ${toNode?.name ?: "Unknown"}\n")
                }
                sb.append("\n")
            }
            
            // Summary statistics
            sb.append("SUMMARY:\n")
            sb.append("- Total Nodes: ${graph.nodes.size}\n")
            sb.append("- Total Edges: ${graph.edges.size}\n")
            sb.append("\n")
        }
        
        // User question
        sb.append("=== USER QUESTION ===\n")
        sb.append(query)
        sb.append("\n")
        
        return sb.toString()
    }
    
    private fun nodeToText(node: Node, graph: GraphData): String {
        val sb = StringBuilder()
        sb.append("Name: ${node.name}\n")
        sb.append("Type: ${node.type}\n")
        sb.append("File: ${node.filePath}\n")
        
        if (node.sensorTypes.isNotEmpty()) {
            sb.append("Sensor Types: ${node.sensorTypes.joinToString(", ") { it.name }}\n")
        }
        
        val incoming = graph.edges.count { it.to == node.id }
        val outgoing = graph.edges.count { it.from == node.id }
        sb.append("Incoming connections: $incoming\n")
        sb.append("Outgoing connections: $outgoing\n")
        
        return sb.toString()
    }
    
    private fun getRelatedNodes(nodeId: String?, graph: GraphData): List<Node> {
        if (nodeId == null) return emptyList()
        
        val relatedIds = mutableSetOf<String>()
        
        // Add directly connected nodes
        graph.edges.forEach { edge ->
            if (edge.from == nodeId) relatedIds.add(edge.to)
            if (edge.to == nodeId) relatedIds.add(edge.from)
        }
        
        return relatedIds.mapNotNull { id -> graph.nodes.find { it.id == id } }
    }
    
    /**
     * Build graph context summary
     */
    private fun buildGraphContext(graph: GraphData?, selectedNodeId: String?): String {
        if (graph == null) return "No graph data available."
        
        val sb = StringBuilder()
        sb.append("=== GRAPH CONTEXT ===\n\n")
        
        // Selected node context
        if (selectedNodeId != null) {
            val selectedNode = graph.nodes.find { it.id == selectedNodeId }
            if (selectedNode != null) {
                sb.append("SELECTED NODE:\n")
                sb.append(nodeToText(selectedNode, graph))
                sb.append("\n")
            }
        }
        
        // Related nodes (limit to 12)
        val relatedNodes = getRelatedNodes(selectedNodeId, graph).take(12)
        if (relatedNodes.isNotEmpty()) {
            sb.append("RELATED NODES:\n")
            relatedNodes.forEach { node ->
                sb.append("- ${node.name} (${node.type})\n")
            }
            sb.append("\n")
        }
        
        // Summary statistics
        sb.append("SUMMARY:\n")
        sb.append("- Total Nodes: ${graph.nodes.size}\n")
        sb.append("- Total Edges: ${graph.edges.size}\n")
        
        return sb.toString()
    }
}
