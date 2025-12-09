package core.ai

import java.io.File

data class LocatedFile(
    val absolutePath: String,
    val relativePath: String,
    val fileContent: String
)

class FileLocator {
    
    /**
     * Locate a file for a given node, using multiple lookup strategies
     */
    fun locateFile(nodeFilePath: String?, nodeName: String?, projectPath: String): LocatedFile? {
        if (projectPath.isBlank()) return null
        
        // Strategy 1: Primary Lookup (Direct path)
        if (!nodeFilePath.isNullOrBlank()) {
            val file = File(nodeFilePath)
            if (file.exists() && file.isFile) {
                return readFile(file, projectPath)
            }
            
            // Try resolving relative to project path if it's a relative path
            val relativeFile = File(projectPath, nodeFilePath)
            if (relativeFile.exists() && relativeFile.isFile) {
                return readFile(relativeFile, projectPath)
            }
        }
        
        // Strategy 2: Secondary Lookup (Standard locations)
        if (!nodeName.isNullOrBlank()) {
            val standardPaths = listOf(
                "src/main/java",
                "src/main/kotlin",
                "app/src/main/java",
                "app/src/main/kotlin"
            )
            
            for (path in standardPaths) {
                val baseDir = File(projectPath, path)
                if (baseDir.exists()) {
                    // Search recursively for file with nodeName
                    val match = baseDir.walkTopDown()
                        .filter { it.isFile }
                        .find { it.name == "$nodeName.kt" || it.name == "$nodeName.java" }
                    
                    if (match != null) {
                        return readFile(match, projectPath)
                    }
                }
            }
        }
        
        // Strategy 3: Fallback Lookup (Fuzzy match by content)
        // This is expensive, so we limit it to .kt and .java files in the project
        if (!nodeName.isNullOrBlank()) {
            val projectDir = File(projectPath)
            if (projectDir.exists()) {
                val match = projectDir.walkTopDown()
                    .maxDepth(10) // Limit depth to avoid deep build dirs
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                    .filter { !it.path.contains("build") && !it.path.contains(".git") } // Exclude build/git
                    .find { file ->
                        try {
                            val content = file.readText()
                            // Look for class/object/fun declaration
                            content.contains("class $nodeName") || 
                            content.contains("object $nodeName") ||
                            content.contains("fun $nodeName")
                        } catch (e: Exception) {
                            false
                        }
                    }
                
                if (match != null) {
                    return readFile(match, projectPath)
                }
            }
        }
        
        return null
    }
    
    private fun readFile(file: File, projectPath: String): LocatedFile {
        val absolutePath = file.absolutePath
        val relativePath = if (absolutePath.startsWith(projectPath)) {
            absolutePath.substring(projectPath.length).trimStart(File.separatorChar)
        } else {
            file.name
        }
        return LocatedFile(absolutePath, relativePath, file.readText())
    }
}
