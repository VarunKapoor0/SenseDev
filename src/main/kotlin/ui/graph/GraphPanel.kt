package ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import core.graph.GraphData
import core.graph.Node
import core.graph.NodeType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GraphPanel(
    graphData: GraphData,
    selectedNodeId: String? = null,
    onNodeSelected: (String) -> Unit = {},
    searchQuery: String = "",
    filterNodeTypes: Set<NodeType> = emptySet(),
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // State for node positions
    var nodePositions by remember(graphData) { 
        mutableStateOf(initializePositions(graphData.nodes)) 
    }
    
    // Viewport state
    var viewportOffset by remember { mutableStateOf(Offset.Zero) }
    
    // Dragging state
    var draggedNodeId by remember { mutableStateOf<String?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    // Filter nodes
    val visibleNodes = remember(graphData, filterNodeTypes, searchQuery) {
        graphData.nodes.filter { node ->
            // Type filter
            (filterNodeTypes.isEmpty() || node.type in filterNodeTypes) &&
            // Search filter (if query exists, only show matching nodes or their neighbors?)
            // For now, let's just highlight search results but keep others visible unless filtered by type
            true
        }
    }
    
    val visibleNodeIds = visibleNodes.map { it.id }.toSet()
    
    // Filter edges
    val visibleEdges = remember(graphData, visibleNodeIds) {
        graphData.edges.filter { edge ->
            edge.from in visibleNodeIds && edge.to in visibleNodeIds
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val clickedNode = nodePositions.entries.find { (id, pos) ->
                            id in visibleNodeIds && ((pos + viewportOffset) - offset).getDistance() < 30f
                        }
                        if (clickedNode != null) {
                            onNodeSelected(clickedNode.key)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val clickedNode = nodePositions.entries.find { (id, pos) ->
                                id in visibleNodeIds && ((pos + viewportOffset) - offset).getDistance() < 30f
                            }
                            draggedNodeId = clickedNode?.key
                        },
                        onDragEnd = { draggedNodeId = null },
                        onDragCancel = { draggedNodeId = null },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (draggedNodeId != null) {
                                val currentPos = nodePositions[draggedNodeId] ?: Offset.Zero
                                val newPositions = nodePositions.toMutableMap()
                                newPositions[draggedNodeId!!] = currentPos + dragAmount
                                nodePositions = newPositions
                            } else {
                                viewportOffset += dragAmount
                            }
                        }
                    )
                }
        ) {
            // Draw Edges
            visibleEdges.forEach { edge ->
                val start = nodePositions[edge.from]
                val end = nodePositions[edge.to]
                
                if (start != null && end != null) {
                    val actualStart = start + viewportOffset
                    val actualEnd = end + viewportOffset
                    
                    // Determine edge color/style based on selection
                    var edgeColor = Color.Gray
                    var strokeWidth = 2f
                    
                    if (selectedNodeId != null) {
                        if (edge.from == selectedNodeId) {
                            edgeColor = Color(0xFF81C784) // Green (Outgoing)
                            strokeWidth = 3f
                        } else if (edge.to == selectedNodeId) {
                            edgeColor = Color(0xFFE57373) // Red (Incoming)
                            strokeWidth = 3f
                        } else {
                            edgeColor = Color.Gray.copy(alpha = 0.3f) // Dim others
                        }
                    }
                    
                    drawLine(
                        color = edgeColor,
                        start = actualStart,
                        end = actualEnd,
                        strokeWidth = strokeWidth
                    )
                    
                    // Draw label if selected or connected to selected
                    if (selectedNodeId == null || edge.from == selectedNodeId || edge.to == selectedNodeId) {
                        val midpoint = (actualStart + actualEnd) / 2f
                        val edgeLabelResult = textMeasurer.measure(
                            text = edge.type.name,
                            style = TextStyle(fontSize = 9.sp, color = Color(0xFFBB86FC), background = Color(0xFF1E1E1E))
                        )
                        drawText(
                            textLayoutResult = edgeLabelResult,
                            topLeft = midpoint - Offset(edgeLabelResult.size.width / 2f, edgeLabelResult.size.height / 2f)
                        )
                    }
                }
            }
            
            // Draw Nodes
            visibleNodes.forEach { node ->
                val pos = nodePositions[node.id] ?: return@forEach
                val actualPos = pos + viewportOffset
                
                var color = getNodeColor(node.type)
                var radius = 20f
                var alpha = 1f
                
                // Search Highlighting
                val isSearchMatch = searchQuery.isNotEmpty() && node.name.contains(searchQuery, ignoreCase = true)
                if (searchQuery.isNotEmpty() && !isSearchMatch) {
                    alpha = 0.3f
                }
                
                // Selection Highlighting
                if (selectedNodeId != null) {
                    if (node.id == selectedNodeId) {
                        radius = 25f
                        alpha = 1f
                    } else {
                        // Check if connected
                        val isConnected = visibleEdges.any { 
                            (it.from == selectedNodeId && it.to == node.id) || 
                            (it.from == node.id && it.to == selectedNodeId) 
                        }
                        if (!isConnected && searchQuery.isEmpty()) {
                            alpha = 0.3f
                        }
                    }
                }
                
                // Draw selection ring
                if (node.id == selectedNodeId) {
                    drawCircle(
                        color = Color.White,
                        radius = radius + 3f,
                        center = actualPos,
                        style = Stroke(width = 3f)
                    )
                }
                
                // Draw search match ring
                if (isSearchMatch) {
                    drawCircle(
                        color = Color.Yellow,
                        radius = radius + 6f,
                        center = actualPos,
                        style = Stroke(width = 2f)
                    )
                }
                
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = actualPos
                )
                
                // Draw label
                val textLayoutResult = textMeasurer.measure(
                    text = node.name,
                    style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = alpha))
                )
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = actualPos + Offset(-textLayoutResult.size.width / 2f, radius + 5f)
                )
            }
        }
    }
}

private fun initializePositions(nodes: List<Node>): Map<String, Offset> {
    val positions = mutableMapOf<String, Offset>()
    val centerX = 400f
    val centerY = 300f
    val radius = 200f
    
    nodes.forEachIndexed { index, node ->
        val angle = (2 * Math.PI * index) / nodes.size
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        positions[node.id] = Offset(x, y)
    }
    
    return positions
}

private fun getNodeColor(type: NodeType): Color {
    return when (type) {
        NodeType.SENSOR_SOURCE -> Color(0xFFE57373) // Red
        NodeType.LOGIC -> Color(0xFF64B5F6)         // Blue
        NodeType.VIEWMODEL -> Color(0xFF81C784)     // Green
        NodeType.UI -> Color(0xFFFFB74D)            // Orange
        NodeType.GENERIC -> Color.LightGray
    }
}
