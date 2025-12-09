package verification

import core.AnalysisEngine
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    val testProjectPath = File("test_project").absolutePath
    println("Verifying analysis on: $testProjectPath")
    
    val engine = AnalysisEngine()
    val result = engine.analyzeProject(testProjectPath)
    
    if (!result.success) {
        println("Analysis failed: ${result.errorMessage}")
        return@runBlocking
    }
    
    println("Analysis successful!")
    println("Nodes: ${result.graph?.nodes?.size}")
    println("Edges: ${result.graph?.edges?.size}")
    println("Flows: ${result.flows.size}")
    
    // Verify Nodes
    val repoNode = result.graph?.nodes?.find { it.name == "TestRepository" }
    val vmNode = result.graph?.nodes?.find { it.name == "TestViewModel" }
    val activityNode = result.graph?.nodes?.find { it.name == "TestActivity" }
    
    println("Found Repository: ${repoNode != null}")
    println("Found ViewModel: ${vmNode != null}")
    println("Found Activity: ${activityNode != null}")
    
    if (repoNode != null && vmNode != null && activityNode != null) {
        // Verify Edges
        val repoToVm = result.graph?.edges?.find { it.from == vmNode.id && it.to == repoNode.id }
        val vmToActivity = result.graph?.edges?.find { it.from == activityNode.id && it.to == vmNode.id }
        
        println("Edge VM -> Repo: ${repoToVm?.type}")
        println("Edge Activity -> VM: ${vmToActivity?.type}")
        
        // Verify Flow
        val flow = result.flows.firstOrNull()
        if (flow != null) {
            println("Flow found: ${flow.sensorType}")
            println("Path: ${flow.path.map { id -> result.graph?.nodes?.find { it.id == id }?.name }}")
        } else {
            println("No flow found!")
        }
    }
}
