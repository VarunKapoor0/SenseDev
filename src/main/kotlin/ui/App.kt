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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import ui.theme.*
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
    var showAboutDialog by remember { mutableStateOf(false) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val analysisCache = remember { AnalysisCache() }
    val coroutineScope = rememberCoroutineScope()
    
    // UI State for Right Panel
    // UI State for Right Panel
    var rightPanelTab by remember { mutableStateOf(0) } // 0 = Details, 1 = AI
    var isAILoading by remember { mutableStateOf(false) }
    var aiSettingsVersion by remember { mutableStateOf(0) }

    SenseDevTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // Menu Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1f),
                color = Color(0xFF1A1A1A),
                elevation = 4.dp // Shadow
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
                    onAbout = {
                        showAboutDialog = true
                    },
                    onExit = {
                        exitProcess(0)
                    },
                    settingsVersion = aiSettingsVersion
                )
            }
            
            // Main Content Area with Resizable Panes
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Ensure layout has at least 800dp width (safe for Sidebar ~250dp + Details ~300dp + Content)
                val minSafeWidth = 800.dp
                val rowModifier = if (maxWidth < minSafeWidth) {
                    Modifier.width(minSafeWidth).fillMaxHeight().horizontalScroll(rememberScrollState())
                } else {
                    Modifier.fillMaxSize()
                }

                Row(modifier = rowModifier) {
                // Resizable Sidebar
                var sidebarWidth by remember { mutableStateOf(250.dp) }
                
                Column(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .background(PanelBackground)
                        .padding(vertical = 16.dp)
                ) {
                    // Open Project Button
                    Button(
                        onClick = { showFileDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF262626)),
                        shape = RoundedCornerShape(8.dp),
                        elevation = ButtonDefaults.elevation(0.dp, 2.dp)
                    ) {
                        Text(
                            if (appState.loadedProjectPath != null) "Change Project" else "Open Project",
                            color = AccentMuted,
                            style = MaterialTheme.typography.button
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Sidebar Section: Project
                    Text(
                        "PROJECT", 
                        style = MaterialTheme.typography.overline, 
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    
                    // Custom Sidebar Item Composable
                     @Composable
                    fun SidebarItem(
                        label: String, 
                        icon: androidx.compose.ui.graphics.vector.ImageVector, 
                        isSelected: Boolean, 
                        onClick: () -> Unit
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clickable(onClick = onClick)
                                .background(if (isSelected) SurfaceHighlight else Color.Transparent)
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) AccentPrimary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.body2.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }

                    // View Navigation
                    SidebarItem("Graph", Icons.Default.AccountTree, appState.currentView == ViewType.GRAPH) { appState.navigateTo(ViewType.GRAPH) }
                    SidebarItem("Code", Icons.Default.Code, appState.currentView == ViewType.CODE) { appState.navigateTo(ViewType.CODE) }
                    SidebarItem("Issues", Icons.Default.BugReport, appState.currentView == ViewType.ISSUES) { appState.navigateTo(ViewType.ISSUES) }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "EXPLORE", 
                        style = MaterialTheme.typography.overline, 
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    
                    // Toggle for Files/AI instead of tabs? Or keep them as views?
                    // Let's integrate Files and AI as part of the view or separate panels?
                    // The old code used tabs [Home, Files, AI].
                    // Let's implement Files and AI as togglable panels or views in the main area?
                    // For now, I'll keep the logic simple: Selecting "Files" shows the file tree in the sidebar? 
                    // Or keep the old "Home/Files/AI" tab logic but styled better?
                    // The prompt says "Sidebar specification... Section Labels GRAPH, CODE, ISSUES".
                    // It seems the sidebar IS the navigation.
                    // But where does the File Tree go? Usually file tree IS the sidebar.
                    // Let's put File Tree in the sidebar below navigation if "Files" is active?
                    // Or maybe "Files" is a view?
                    
                    // Let's assume for this redesign:
                    // Sidebar has navigation. Content area shows Graph/Code/Issues.
                    // But we need the File Tree somewhere.
                    // Let's add a "Files" item. If selected, changing the sidebar content to file tree might be complex.
                    // Strategy: Keep "File Tree" as a persistent section in Sidebar for now, or a toggle.
                    // Let's just add "Files" and "AI Assistant" as items.
                    
                    var showFiles by remember { mutableStateOf(false) }
                    SidebarItem("Project Files", Icons.Default.Folder, showFiles) { 
                        showFiles = !showFiles
                    }
                    
                    if (showFiles && appState.loadedProjectPath != null) {
                        Box(modifier = Modifier.weight(1f)) {
                             ui.components.FolderTreePanel(
                                rootPath = appState.loadedProjectPath!!,
                                selectedFile = appState.currentCodeFile?.let { File(it) },
                                onFileSelected = { file ->
                                    println("Selected file: ${file.absolutePath}")
                                    // Also navigate to Code view
                                    appState.openCode(file.absolutePath)
                                }
                            )
                        }
                    }
                    
                    // If files not shown, spacer
                    if (!showFiles) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    

                     SidebarItem("AI Assistant", Icons.Default.AutoAwesome, rightPanelTab == 1) { 
                        rightPanelTab = 1
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
                    // Show content if we have analysis result OR a loaded project
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
                        .background(PanelBackground)
                        .padding(16.dp)
                ) {
                    // Tab Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(Color(0xFF1A1A1A))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { rightPanelTab = 0 }
                                .background(if (rightPanelTab == 0) Color(0xFF262626) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DETAILS", color = if (rightPanelTab == 0) TextPrimary else TextSecondary, style = MaterialTheme.typography.button)
                            if (rightPanelTab == 0) Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(AccentPrimary))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { rightPanelTab = 1 }
                                .background(if (rightPanelTab == 1) Color(0xFF262626) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AI", color = if (rightPanelTab == 1) TextPrimary else TextSecondary, style = MaterialTheme.typography.button)
                            if (rightPanelTab == 1) Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(AccentPrimary))
                        }
                    }
                    
                    Divider(color = DividerColor)
                    
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (rightPanelTab == 0) {
                             Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                                 Spacer(modifier = Modifier.height(8.dp))
                                 
                                // Show details if we have analysis result
                                if (appState.analysisResult != null) {
                        val result = appState.analysisResult
                        if (result != null && result.graph != null && selectedNodeId != null) {
                            // Find selected node
                            val selectedNode = result.graph.nodes.find { it.id == selectedNodeId }
                            if (selectedNode != null) {
                                    Text(
                                        "DETAILS",
                                        style = MaterialTheme.typography.overline,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    // Title
                                    Text(
                                        selectedNode.name,
                                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Accent Underline
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(2.dp)
                                            .background(AccentPrimary)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Metadata Badges
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Type Badge
                                        Surface(
                                            color = SurfaceHighlight,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, BorderColor)
                                        ) {
                                            Text(
                                                selectedNode.type.name,
                                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                                                color = ui.theme.AccentMuted,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // View Source Button
                                    Button(
                                        onClick = {
                                            appState.openCode(selectedNode.filePath)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = AccentMuted.copy(alpha = 0.2f)),
                                        elevation = ButtonDefaults.elevation(0.dp, 0.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("View Source", color = AccentPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    // Sections
                                    if (selectedNode.sensorTypes.isNotEmpty()) {
                                        Text("SENSORS", style = MaterialTheme.typography.overline, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        selectedNode.sensorTypes.forEach { sensorType ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Sensors, contentDescription = null, tint = AccentMuted, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(sensorType.name, style = MaterialTheme.typography.body2, color = TextPrimary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    
                                    // Incoming edges
                                    val incomingEdges = result.graph.edges.filter { it.to == selectedNodeId }
                                    if (incomingEdges.isNotEmpty()) {
                                        Text("INCOMING (${incomingEdges.size})", style = MaterialTheme.typography.overline, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        incomingEdges.take(10).forEach { edge ->
                                            val fromNode = result.graph.nodes.find { it.id == edge.from }
                                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                 Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(12.dp))
                                                 Spacer(modifier = Modifier.width(8.dp))
                                                 Text(fromNode?.name ?: "Unknown", style = MaterialTheme.typography.body2, color = TextPrimary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    
                                    // Outgoing edges
                                    val outgoingEdges = result.graph.edges.filter { it.from == selectedNodeId }
                                    if (outgoingEdges.isNotEmpty()) {
                                        Text("OUTGOING (${outgoingEdges.size})", style = MaterialTheme.typography.overline, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        outgoingEdges.take(10).forEach { edge ->
                                            val toNode = result.graph.nodes.find { it.id == edge.to }
                                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                 Icon(Icons.Default.ArrowForward, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(12.dp))
                                                 Spacer(modifier = Modifier.width(8.dp))
                                                 Text(toNode?.name ?: "Unknown", style = MaterialTheme.typography.body2, color = TextPrimary)
                                            }
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
                                    } // End if analysisResult
                             } // End Scrollable Column
                        } else {
                            // AI Panel
                           ui.ai.AIPanel(
                                messages = appState.chatMessages,
                                isLoading = isAILoading,
                                settingsVersion = aiSettingsVersion,
                                onSendMessage = { query ->
                                    appState.addChatMessage(
                                        core.ai.ChatMessage(
                                            role = core.ai.MessageRole.USER,
                                            content = query
                                        )
                                    )
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
                    } // End Box content
                } // End Right Panel Column
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
            },
            onSettingsChanged = {
                aiSettingsVersion++
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        ui.components.AboutWindow(
            onDismiss = {
                showAboutDialog = false
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
                .background(ui.theme.DividerColor)
                .align(Alignment.Center)
        )
    }
}
