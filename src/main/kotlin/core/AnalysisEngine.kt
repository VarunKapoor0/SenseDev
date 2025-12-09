package core

import core.graph.*
import core.indexer.ProjectIndexer
import core.issues.Issue
import core.issues.IssueDetector
import core.model.ClassSymbol
import core.model.SymbolMap
import core.parser.JavaParserAdapter
import core.parser.KotlinParser
import core.sensor.SensorDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Main analysis engine that orchestrates the entire pipeline
 */
class AnalysisEngine {
    
    private val projectIndexer = ProjectIndexer()
    private val kotlinParser = KotlinParser()
    private val javaParser = JavaParserAdapter()
    private val sensorDetector = SensorDetector()
    private val graphBuilder = GraphBuilder()
    private val flowExtractor = FlowExtractor()
    private val issueDetector = IssueDetector()
    
    /**
     * Analyze a project and return results
     */
    suspend fun analyzeProject(
        projectPath: String,
        onProgress: (AnalysisProgress) -> Unit = {}
    ): AnalysisResult = withContext(Dispatchers.Default) {
        try {
            // Step 1: Index project
            onProgress(AnalysisProgress(0.1f, "Scanning project files..."))
            val projectIndex = projectIndexer.indexProject(projectPath)
            
            if (projectIndex.totalFiles == 0) {
                return@withContext AnalysisResult(
                    success = false,
                    errorMessage = "No Kotlin or Java files found in project"
                )
            }
            
            // Step 2: Parse files
            onProgress(AnalysisProgress(0.3f, "Parsing ${projectIndex.totalFiles} files..."))
            val allClasses = mutableListOf<ClassSymbol>()
            
            for (kotlinFile in projectIndex.kotlinFiles) {
                try {
                    val classes = kotlinParser.parseFile(kotlinFile)
                    allClasses.addAll(classes)
                } catch (e: Exception) {
                    println("Error parsing $kotlinFile: ${e.message}")
                }
            }
            
            for (javaFile in projectIndex.javaFiles) {
                try {
                    val classes = javaParser.parseFile(javaFile)
                    allClasses.addAll(classes)
                } catch (e: Exception) {
                    println("Error parsing $javaFile: ${e.message}")
                }
            }
            
            // Build symbol map
            val classMap = allClasses.associateBy { it.qualifiedName }
            val fileMap = allClasses.groupBy { it.filePath }
                .mapValues { (_, classes) -> classes.map { it.qualifiedName } }
            val symbolMap = SymbolMap(classMap, fileMap)
            
            // Step 3: Detect sensors
            onProgress(AnalysisProgress(0.5f, "Detecting sensor API usage..."))
            val sensorEntryPoints = sensorDetector.detectSensorUsage(allClasses)
            
            // Step 4: Build graph
            onProgress(AnalysisProgress(0.7f, "Building call graph..."))
            val graphData = graphBuilder.buildGraph(symbolMap, sensorEntryPoints)
            
            // Step 5: Extract flows
            onProgress(AnalysisProgress(0.8f, "Extracting data flows..."))
            val flows = flowExtractor.extractFlows(graphData)
            
            // Step 6: Detect issues
            onProgress(AnalysisProgress(0.9f, "Detecting issues..."))
            val issues = issueDetector.detectIssues(symbolMap, graphData)
            
            onProgress(AnalysisProgress(1.0f, "Analysis complete"))
            
            AnalysisResult(
                success = true,
                projectPath = projectPath,
                graph = graphData,
                flows = flows,
                issues = issues,
                totalFiles = projectIndex.totalFiles,
                totalClasses = allClasses.size,
                sensorCount = sensorEntryPoints.size
            )
        } catch (e: Exception) {
            AnalysisResult(
                success = false,
                errorMessage = "Analysis failed: ${e.message}"
            )
        }
    }
}

/**
 * Analysis progress update
 */
data class AnalysisProgress(
    val progress: Float, // 0.0 to 1.0
    val message: String
)

/**
 * Result of project analysis
 */
@Serializable
data class AnalysisResult(
    val success: Boolean,
    val projectPath: String? = null,  // Path to the analyzed project
    val graph: GraphData? = null,
    val flows: List<Flow> = emptyList(),
    val issues: List<Issue> = emptyList(),
    val totalFiles: Int = 0,
    val totalClasses: Int = 0,
    val sensorCount: Int = 0,
    val errorMessage: String? = null
)
