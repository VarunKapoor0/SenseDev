package core.cache

import core.AnalysisResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Handles saving and loading analysis results to/from disk
 */
class AnalysisCache {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * Save analysis result to a file
     */
    fun saveAnalysis(result: AnalysisResult, projectPath: String): Boolean {
        return try {
            val cacheDir = File(projectPath, ".sensedev/cache")
            cacheDir.mkdirs()
            
            val cacheFile = File(cacheDir, "analysis.json")
            val jsonString = json.encodeToString(result)
            cacheFile.writeText(jsonString)
            
            println("Analysis saved to: ${cacheFile.absolutePath}")
            true
        } catch (e: Exception) {
            println("Failed to save analysis: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Load analysis result from a file
     */
    fun loadAnalysis(projectPath: String): AnalysisResult? {
        return try {
            val cacheFile = File(projectPath, ".sensedev/cache/analysis.json")
            
            if (!cacheFile.exists()) {
                println("No cached analysis found at: ${cacheFile.absolutePath}")
                return null
            }
            
            val jsonString = cacheFile.readText()
            val result = json.decodeFromString<AnalysisResult>(jsonString)
            
            println("Analysis loaded from: ${cacheFile.absolutePath}")
            result
        } catch (e: Exception) {
            println("Failed to load analysis: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if cached analysis exists for a project
     */
    fun hasCachedAnalysis(projectPath: String): Boolean {
        val cacheFile = File(projectPath, ".sensedev/cache/analysis.json")
        return cacheFile.exists()
    }
    
    /**
     * Delete cached analysis
     */
    fun clearCache(projectPath: String): Boolean {
        return try {
            val cacheFile = File(projectPath, ".sensedev/cache/analysis.json")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            true
        } catch (e: Exception) {
            println("Failed to clear cache: ${e.message}")
            false
        }
    }
}
