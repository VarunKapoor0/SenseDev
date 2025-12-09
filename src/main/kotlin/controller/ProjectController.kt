package controller

import core.AnalysisEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import state.AppState
import java.io.File

class ProjectController(private val appState: AppState) {

    private val analysisEngine = AnalysisEngine()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun openProject(path: String) {
        val file = File(path)
        if (file.exists() && file.isDirectory) {
            appState.loadProject(path)
            println("Project loaded: $path")
            
            // Trigger analysis
            analyzeProject(path)
        } else {
            println("Invalid project path: $path")
        }
    }

    fun loadProject(path: String) {
        val file = File(path)
        if (file.exists() && file.isDirectory) {
            appState.loadProject(path)
            println("Project loaded (no analysis): $path")
        }
    }

    private fun analyzeProject(path: String) {
        appState.updateAnalyzing(true)
        appState.updateAnalysisProgress(0f, "Starting analysis...")

        coroutineScope.launch {
            val result = analysisEngine.analyzeProject(path) { progress ->
                appState.updateAnalysisProgress(progress.progress, progress.message)
            }

            appState.updateAnalysisResult(result)
            appState.updateAnalyzing(false)

            if (result.success) {
                println("Analysis complete: ${result.totalClasses} classes, ${result.sensorCount} sensor entry points, ${result.issues.size} issues")
            } else {
                println("Analysis failed: ${result.errorMessage}")
            }
        }
    }
}
