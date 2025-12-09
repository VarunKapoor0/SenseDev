package state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import core.AnalysisResult

class AppState {
    var loadedProjectPath by mutableStateOf<String?>(null)
        private set

    var currentView by mutableStateOf(ViewType.GRAPH)
        private set

    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    var isAnalyzing by mutableStateOf(false)
        private set

    var analysisProgress by mutableStateOf(0f)
        private set

    var analysisMessage by mutableStateOf("")
        private set

    var currentCodeFile by mutableStateOf<String?>(null)
        private set

    var currentCodeLine by mutableStateOf<Int?>(null)
        private set

    // Graph Filters
    var filterNodeTypes by mutableStateOf<Set<core.graph.NodeType>>(emptySet())
        private set
        
    var filterSensorTypes by mutableStateOf<Set<String>>(emptySet())
        private set
        
    var searchQuery by mutableStateOf("")
        private set
        
    // AI State
    var chatMessages = mutableStateListOf<core.ai.ChatMessage>()
        private set
        
    val aiEngine = core.ai.AIEngine()

    fun loadProject(path: String) {
        loadedProjectPath = path
    }

    fun navigateTo(view: ViewType) {
        currentView = view
    }
    
    fun updateFilters(nodeTypes: Set<core.graph.NodeType>, sensorTypes: Set<String>) {
        filterNodeTypes = nodeTypes
        filterSensorTypes = sensorTypes
    }
    
    fun updateSearchQuery(query: String) {
        searchQuery = query
    }
    
    fun addChatMessage(message: core.ai.ChatMessage) {
        chatMessages.add(message)
    }

    fun openCode(filePath: String, lineNumber: Int? = null) {
        currentCodeFile = filePath
        currentCodeLine = lineNumber
        currentView = ViewType.CODE
    }

    fun updateAnalysisResult(result: AnalysisResult?) {
        analysisResult = result
    }

    fun updateAnalyzing(analyzing: Boolean) {
        isAnalyzing = analyzing
    }

    fun updateAnalysisProgress(progress: Float, message: String) {
        analysisProgress = progress
        analysisMessage = message
    }
}

enum class ViewType {
    GRAPH,
    CODE,
    ISSUES
}
