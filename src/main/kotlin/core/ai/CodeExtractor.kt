package core.ai

import core.graph.GraphData
import core.graph.Node
import java.io.File

class CodeExtractor {
    
    /**
     * Extract code snippet based on access level and context
     */
    fun extractCode(
        nodeId: String?,
        graph: GraphData?,
        projectPath: String?,
        accessLevel: CodeAccessLevel,
        maxTokens: Int
    ): String {
        if (nodeId == null || graph == null || projectPath == null) {
            return ""
        }
        
        val node = graph.nodes.find { it.id == nodeId } ?: return ""
        
        return when (accessLevel) {
            CodeAccessLevel.SNIPPET_ONLY -> extractSnippet(node, projectPath, maxTokens)
            CodeAccessLevel.FLOW_LEVEL -> extractFlowContext(node, graph, projectPath, maxTokens)
            CodeAccessLevel.FULL_FILE -> extractFullFile(node, projectPath, maxTokens)
        }
    }
    
    private val fileLocator = FileLocator()

    /**
     * Extract just the selected method/class (minimal context)
     */
    private fun extractSnippet(node: Node, projectPath: String, maxTokens: Int): String {
        val locatedFile = fileLocator.locateFile(node.filePath, node.name, projectPath)
        
        if (locatedFile == null) {
            return "// File not found for node: ${node.name} (Path: ${node.filePath})"
        }
        
        val content = locatedFile.fileContent
        val filePath = locatedFile.relativePath
        
        // Simple heuristic: try to extract the method/class around the node
        // For now, just return a portion of the file
        val lines = content.lines()
        val targetName = node.name
        
        // Find the declaration line
        val declarationLine = lines.indexOfFirst { it.contains(targetName) }
        if (declarationLine == -1) {
            // Fallback: if we found the file but not the specific declaration, return the top of the file
            // or return the whole file if it's small enough
            if (estimateTokens(content) <= maxTokens) {
                 return """
// File: $filePath
// Note: Could not locate exact definition of ${node.name}, showing full file.

$content
                 """.trimIndent()
            }
            return "// Could not locate definition of ${node.name} in $filePath"
        }
        
        // Extract context (e.g., 20 lines before and after)
        val start = maxOf(0, declarationLine - 5)
        val end = minOf(lines.size, declarationLine + 30)
        
        val snippet = lines.subList(start, end).joinToString("\n")
        
        return truncateToTokens("""
// File: $filePath
// Snippet around: ${node.name}

$snippet
        """.trimIndent(), maxTokens / 2) // Reserve half tokens for snippet
    }
    
    /**
     * Extract code from flow-connected nodes (multi-file)
     */
    private fun extractFlowContext(node: Node, graph: GraphData, projectPath: String, maxTokens: Int): String {
        val sb = StringBuilder()
        val visited = mutableSetOf<String>()
        
        // Get connected nodes (1 hop)
        val connectedNodeIds = mutableSetOf<String>()
        graph.edges.forEach { edge ->
            if (edge.from == node.id) connectedNodeIds.add(edge.to)
            if (edge.to == node.id) connectedNodeIds.add(edge.from)
        }
        
        // Add main node
        sb.append(extractSnippet(node, projectPath, maxTokens / 3))
        visited.add(node.id)
        
        // Add connected nodes (budget remaining tokens)
        val remainingTokens = maxTokens - estimateTokens(sb.toString())
        var tokensPerNode = if (connectedNodeIds.size > 0) remainingTokens / connectedNodeIds.size else 0
        tokensPerNode = minOf(tokensPerNode, 500) // Max 500 tokens per related node
        
        connectedNodeIds.take(5).forEach { connectedId ->
            val connectedNode = graph.nodes.find { it.id == connectedId }
            if (connectedNode != null && connectedNode.id !in visited) {
                sb.append("\n\n// --- Connected Node ---\n")
                sb.append(extractSnippet(connectedNode, projectPath, tokensPerNode))
                visited.add(connectedNode.id)
            }
        }
        
        return truncateToTokens(sb.toString(), maxTokens)
    }
    
    /**
     * Extract entire file content (with truncation if needed)
     */
    private fun extractFullFile(node: Node, projectPath: String, maxTokens: Int): String {
        val locatedFile = fileLocator.locateFile(node.filePath, node.name, projectPath)
        
        if (locatedFile == null) {
            return "// File not found for node: ${node.name}"
        }
        
        val content = """
// Full File: ${locatedFile.relativePath}

${locatedFile.fileContent}
        """.trimIndent()
        
        return truncateToTokens(content, maxTokens)
    }
    
    /**
     * Estimate tokens (rough approximation: 1 token ≈ 4 characters)
     */
    fun estimateTokens(code: String): Int {
        return code.length / 4
    }
    
    /**
     * Truncate code to fit within token limit
     */
    private fun truncateToTokens(code: String, maxTokens: Int): String {
        val estimatedTokens = estimateTokens(code)
        if (estimatedTokens <= maxTokens) return code
        
        val maxChars = maxTokens * 4
        val truncated = code.take(maxChars)
        return "$truncated\n\n// ... (truncated to fit token limit)"
    }
}
