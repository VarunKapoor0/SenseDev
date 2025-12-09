package core.indexer

import core.model.SymbolMap
import java.io.File

/**
 * Scans Android project directory and identifies source files
 */
class ProjectIndexer {
    
    /**
     * Index project and return list of Kotlin and Java files
     */
    fun indexProject(projectPath: String): ProjectIndex {
        val projectDir = File(projectPath)
        
        if (!projectDir.exists() || !projectDir.isDirectory) {
            return ProjectIndex(emptyList(), emptyList())
        }
        
        val kotlinFiles = mutableListOf<String>()
        val javaFiles = mutableListOf<String>()
        
        scanDirectory(projectDir, kotlinFiles, javaFiles)
        
        return ProjectIndex(kotlinFiles, javaFiles)
    }
    
    private fun scanDirectory(
        directory: File,
        kotlinFiles: MutableList<String>,
        javaFiles: MutableList<String>
    ) {
        val files = directory.listFiles() ?: return
        
        for (file in files) {
            // Skip build directories and hidden files
            if (file.name.startsWith(".") || 
                file.name == "build" || 
                file.name == "gradle" ||
                file.name == ".gradle" ||
                file.name == ".idea") {
                continue
            }
            
            if (file.isDirectory) {
                scanDirectory(file, kotlinFiles, javaFiles)
            } else {
                when (file.extension.lowercase()) {
                    "kt" -> kotlinFiles.add(file.absolutePath)
                    "java" -> javaFiles.add(file.absolutePath)
                }
            }
        }
    }
}

/**
 * Result of project indexing
 */
data class ProjectIndex(
    val kotlinFiles: List<String>,
    val javaFiles: List<String>
) {
    val totalFiles: Int
        get() = kotlinFiles.size + javaFiles.size
}
