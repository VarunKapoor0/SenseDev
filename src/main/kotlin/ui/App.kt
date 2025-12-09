package ui 

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import controller.ProjectController
import core.AnalysisResult
import core.cache.AnalysisCache
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import state.AppState
import state.ViewType
import ui.theme.SenseDevTheme
import ui.components.SaveFileDialog
import ui.components.LoadFileDialog
import java.io.File
import kotlin.system.exitProcess

@Composable
fun App(appState: AppState, projectController: ProjectController) {
    var showFileDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val analysisCache = remember { AnalysisCache() }
    val coroutineScope = rememberCoroutineScope()

    SenseDevTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // Menu Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colors.surface,
                elevation = 4.dp
            ) {
                ui.components.MenuBar(
                    onSaveAnalysis = {
                        val result = appState.analysisResult
                        val projectPath = appState.loadedProjectPath
                        if (result != null && projectPath != null) {
                            if (analysisCache.saveAnalysis(result, projectPath)) {
                                println("Analysis saved to project directory")
                            }
                        }
                    },
                    onSaveAnalysisAs = {
                        showSaveAsDialog = true
                    },
                    onLoadAnalysis = {
                        showLoadDialog = true
                    },
                    onSettings = {
                        showSettingsDialog = true
                    },
                    onExit = {
                        exitProcess(0)
                    },
                    enabled = appState.analysisResult != null
                )
            }
            
            // Main Content Area with Resizable Panes
            Row(modifier = Modifier.fillMaxSize()) {
                // Resizable Sidebar
                var sidebarWidth by remember { mutableStateOf(250.dp) }
                
                Column(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colors.surface)
                ) {
                    var selectedTab by remember { mutableStateOf(0) }
                    
                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Home", color = Color.White) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Files", color = Color.White) },
                            enabled = appState.loadedProjectPath != null
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("AI", color = Color.White) },
                            enabled = appState.analysisResult != null
                        )
                    }

                    // Tab Content
                    when (selectedTab) {
                        0 -> {
                            // Home Tab
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("SenseDev", style = MaterialTheme.typography.h5, color = MaterialTheme.colors.primary)
                                Spacer(modifier = Modifier.height(24.dp))

                                Button(onClick = {
                                    showFileDialog = true
                                }) {
                                    Text("Open Project")
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Show loaded project path
                                if (appState.loadedProjectPath != null) {
                                    Column {
                                        Text("Loaded:", style = MaterialTheme.typography.caption, color = Color.White)
                                        Text(
                                            File(appState.loadedProjectPath!!).name,
                                            style = MaterialTheme.typography.body2,
                                            color = MaterialTheme.colors.secondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text("Views", style = MaterialTheme.typography.subtitle1, color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                ViewType.values().forEach { view ->
                                    TextButton(
                                        onClick = { appState.navigateTo(view) },
                                        enabled = appState.loadedProjectPath != null,
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (appState.currentView == view) {
                                                MaterialTheme.colors.secondary
                                            } else {
                                                Color.White
                                            }
                                        )
                                    ) {
                                        Text(view.name)
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Folder Structure Tab
                            if (appState.loadedProjectPath != null) {
                                ui.components.FolderTreePanel(
                                    rootPath = appState.loadedProjectPath!!,
                                    onFileSelected = { file ->
                                        println("Selected file: ${file.absolutePath}")
                                    }
                                )
                            }
                        }
                        2 -> {
                            // AI Tab
                            if (appState.analysisResult != null) {
                                var isAILoading by remember { mutableStateOf(false) }
                                
                                ui.ai.AIPanel(
                                    messages = appState.chatMessages,
                                    isLoading = isAILoading,
                                    onSendMessage = { query ->
                                        // Add user message
                                        appState.addChatMessage(
                                            core.ai.ChatMessage(
                                                role = core.ai.MessageRole.USER,
                                                content = query
                                            )
                                        )
                                        
                                        // Launch AI processing
                                        coroutineScope.launch {
                                            isAILoading = true
                                            try {
                                                val context = core.ai.AIContext(
                                                    selectedNodeId = selectedNodeId,
                                                    graphData = appState.analysisResult?.graph,
                                                    projectPath = appState.loadedProjectPath
                                                )
                                                val response = appState.aiEngine.processQuery(query, context)
                                                appState.addChatMessage(response)
                                            } finally {
                                                isAILoading = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Left Splitter
                VerticalSplitter(
                    onDrag = { delta ->
                        sidebarWidth = (sidebarWidth + delta.dp).coerceIn(100.dp, 500.dp)
                    }
                )

                // Main Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colors.background)
                        .padding(16.dp)
                ) {
                    // Show content if we have an analysis result OR a loaded project
                    if (appState.loadedProjectPath == null && appState.analysisResult == null) {
                        // Welcome Screen
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Welcome to SenseDev",
                                style = MaterialTheme.typography.h4,
                                color = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Open an Android project to begin analysis",
                                style = MaterialTheme.typography.body1,
                                color = Color.White
                            )
                        }
                    } else if (appState.isAnalyzing) {
                        // Analysis Progress
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(appState.analysisMessage, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = appState.analysisProgress,
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    } else {
                        // View Content
                        when (appState.currentView) {
                            ViewType.GRAPH -> {
                                val result = appState.analysisResult
                                if (result != null && result.success && result.graph != null) {
                                    Column {
                                        ui.graph.GraphToolbar(
                                            searchQuery = appState.searchQuery,
                                            onSearchQueryChanged = { appState.updateSearchQuery(it) },
                                            activeNodeFilters = appState.filterNodeTypes,
                                            onNodeFilterChanged = { appState.updateFilters(it, appState.filterSensorTypes) }
                                        )
                                        
                                        ui.graph.GraphPanel(
                                            graphData = result.graph,
                                            selectedNodeId = selectedNodeId,
                                            onNodeSelected = { nodeId -> selectedNodeId = nodeId },
                                            searchQuery = appState.searchQuery,
                                            filterNodeTypes = appState.filterNodeTypes
                                        )
                                    }
                                } else if (result != null && !result.success) {
                                    Text("Analysis Failed: ${result.errorMessage}", color = MaterialTheme.colors.error)
                                } else {
                                    Text("No analysis data available", color = Color.White)
                                }
                            }
                            ViewType.CODE -> {
                                if (appState.currentCodeFile != null) {
                                    ui.code.CodePanel(
                                        filePath = appState.currentCodeFile!!,
                                        lineNumber = appState.currentCodeLine
                                    )
                                } else {
                                    Text("No file selected", color = Color.White)
                                }
                            }
                            ViewType.ISSUES -> {
                                val result = appState.analysisResult
                                if (result != null) {
                                    ui.issues.IssuesPanel(
                                        issues = result.issues,
                                        onIssueSelected = { issue ->
                                            // Navigate to code if reference exists
                                            val ref = issue.codeRefs.firstOrNull()
                                            if (ref != null) {
                                                appState.openCode(ref.filePath, ref.lineNumber)
                                            } else if (issue.nodeRefs.isNotEmpty()) {
                                                // Fallback to node location if no direct code ref
                                                val nodeId = issue.nodeRefs.first()
                                                val node = result.graph?.nodes?.find { it.id == nodeId }
                                                if (node != null) {
                                                    appState.openCode(node.filePath)
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    Text("No analysis data available", modifier = Modifier.align(Alignment.Center), color = Color.White)
                                }
                            }
                        }
                    }
                }
                
                // Right Splitter
                var detailsWidth by remember { mutableStateOf(300.dp) }
                
                VerticalSplitter(
                    onDrag = { delta ->
                        detailsWidth = (detailsWidth - delta.dp).coerceIn(200.dp, 600.dp)
                    }
                )
                
                // Details Panel (Right side)
                Column(
                    modifier = Modifier
                        .width(detailsWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colors.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        "Details",
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Show details if we have analysis result
                    if (appState.analysisResult != null) {
                        val result = appState.analysisResult
                        if (result != null && result.graph != null && selectedNodeId != null) {
                            // Find selected node
                            val selectedNode = result.graph.nodes.find { it.id == selectedNodeId }
                            if (selectedNode != null) {
                                // Node details
                                Text(
                                    selectedNode.name,
                                    style = MaterialTheme.typography.h6,
                                    color = MaterialTheme.colors.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Type: ${selectedNode.type.name}",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // View Source Button
                                Button(
                                    onClick = {
                                        appState.openCode(selectedNode.filePath)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                                ) {
                                    Text("View Source")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (selectedNode.sensorTypes.isNotEmpty()) {
                                    Text(
                                        "Sensors:",
                                        style = MaterialTheme.typography.subtitle2,
                                        color = MaterialTheme.colors.primary
                                    )
                                    selectedNode.sensorTypes.forEach { sensorType ->
                                        Text(
                                            "• ${sensorType.name}",
                                            style = MaterialTheme.typography.body2,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                
                                // Incoming edges
                                val incomingEdges = result.graph.edges.filter { it.to == selectedNodeId }
                                if (incomingEdges.isNotEmpty()) {
                                    Text(
                                        "Incoming (${incomingEdges.size}):",
                                        style = MaterialTheme.typography.subtitle2,
                                        color = MaterialTheme.colors.primary
                                    )
                                    incomingEdges.take(5).forEach { edge ->
                                        val fromNode = result.graph.nodes.find { it.id == edge.from }
                                        Text(
                                            "← ${fromNode?.name ?: "Unknown"} (${edge.type.name})",
                                            style = MaterialTheme.typography.caption,
                                            color = Color(0xFFBB86FC)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                
                                // Outgoing edges
                                val outgoingEdges = result.graph.edges.filter { it.from == selectedNodeId }
                                if (outgoingEdges.isNotEmpty()) {
                                    Text(
                                        "Outgoing (${outgoingEdges.size}):",
                                        style = MaterialTheme.typography.subtitle2,
                                        color = MaterialTheme.colors.primary
                                    )
                                    outgoingEdges.take(5).forEach { edge ->
                                        val toNode = result.graph.nodes.find { it.id == edge.to }
                                        Text(
                                            "→ ${toNode?.name ?: "Unknown"} (${edge.type.name})",
                                            style = MaterialTheme.typography.caption,
                                            color = Color(0xFF03DAC6)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                
                                // Flows through this node
                                val flowsThroughNode = result.flows.filter { flow ->
                                    selectedNodeId in flow.path
                                }
                                if (flowsThroughNode.isNotEmpty()) {
                                    Text(
                                        "Data Flows (${flowsThroughNode.size}):",
                                        style = MaterialTheme.typography.subtitle2,
                                        color = MaterialTheme.colors.primary
                                    )
                                    flowsThroughNode.forEach { flow ->
                                        Text(
                                            "${flow.sensorType.name} (confidence: ${(flow.confidence * 100).toInt()}%)",
                                            style = MaterialTheme.typography.caption,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                "Analysis Summary",
                                style = MaterialTheme.typography.subtitle1,
                                color = MaterialTheme.colors.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (result != null) {
                                Text(
                                    "Nodes: ${result.graph?.nodes?.size ?: 0}",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.White
                                )
                                Text(
                                    "Edges: ${result.graph?.edges?.size ?: 0}",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.White
                                )
                                Text(
                                    "Flows: ${result.flows.size}",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                "Click a node in the graph to view details.",
                                style = MaterialTheme.typography.caption,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // File Dialog
    if (showFileDialog) {
        FileChooserDialog(
            onDirectorySelected = { path ->
                projectController.openProject(path)
                showFileDialog = false
            },
            onDismiss = {
                showFileDialog = false
            }
        )
    }
    
    // Save As Dialog
    if (showSaveAsDialog) {
        SaveFileDialog(
            onFileSelected = { filePath ->
                val result = appState.analysisResult
                if (result != null) {
                    val analysisCache = AnalysisCache()
                    // Save to custom location by writing directly
                    try {
                        val json = kotlinx.serialization.json.Json {
                            prettyPrint = true
                            ignoreUnknownKeys = true
                        }
                        val jsonString = json.encodeToString<AnalysisResult>(result)
                        File(filePath).writeText(jsonString)
                        println("Analysis saved to: $filePath")
                    } catch (e: Exception) {
                        println("Failed to save: ${e.message}")
                    }
                }
                showSaveAsDialog = false
            },
            onDismiss = {
                showSaveAsDialog = false
            }
        )
    }
    
    // Load Dialog
    if (showLoadDialog) {
        LoadFileDialog(
            onFileSelected = { filePath ->
                try {
                    val jsonString = File(filePath).readText()
                    val json = kotlinx.serialization.json.Json {
                        prettyPrint = true
                        ignoreUnknownKeys = true
                    }
                    val result = json.decodeFromString<AnalysisResult>(jsonString)
                    appState.updateAnalysisResult(result)
                    
                    // Load project folder structure if available
                    result.projectPath?.let { projectPath ->
                        if (File(projectPath).exists()) {
                            projectController.loadProject(projectPath)
                            println("Project loaded from: $projectPath")
                        }
                    }
                    
                    println("Analysis loaded from: $filePath")
                } catch (e: Exception) {
                    println("Failed to load: ${e.message}")
                    e.printStackTrace()
                }
                showLoadDialog = false
            },
            onDismiss = {
                showLoadDialog = false
            }
        )
    }
    
    // Settings Dialog
    if (showSettingsDialog) {
        ui.components.SettingsDialog(
            onDismiss = {
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun FileChooserDialog(
    onDirectorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        val fileChooser = javax.swing.JFileChooser()
        fileChooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        fileChooser.dialogTitle = "Select Android Project Directory"
        
        val result = fileChooser.showOpenDialog(null)
        
        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            val selectedPath = fileChooser.selectedFile.absolutePath
            onDirectorySelected(selectedPath)
        } else {
            onDismiss()
        }
    }
}

@Composable
fun GraphViewPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Graph Visualization Area", style = MaterialTheme.typography.h6, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("(Phase 2: Coming Soon)", style = MaterialTheme.typography.caption, color = Color.White)
        }
    }
}

@Composable
fun CodeViewPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Code View", style = MaterialTheme.typography.h6, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("(Phase 2: Coming Soon)", style = MaterialTheme.typography.caption, color = Color.White)
        }
    }
}

@Composable
fun IssuesViewPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Issues Panel", style = MaterialTheme.typography.h6, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("(Phase 2: Coming Soon)", style = MaterialTheme.typography.caption, color = Color.White)
        }
    }
}

@Composable
fun VerticalSplitter(
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(8.dp)
            .fillMaxHeight()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0xFF3E3E3E))
                .align(Alignment.Center)
        )
    }
}
