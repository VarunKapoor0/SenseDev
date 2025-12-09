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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.graph.GraphData
import core.graph.Node
import core.graph.NodeType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import ui.theme.*

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
                            id in visibleNodeIds && 
                            // Simple rect hit test (assuming 160x80 node approx)
                            offset.x >= (pos.x + viewportOffset.x) - 80f &&
                            offset.x <= (pos.x + viewportOffset.x) + 80f &&
                            offset.y >= (pos.y + viewportOffset.y) - 25f &&
                            offset.y <= (pos.y + viewportOffset.y) + 25f
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
                                id in visibleNodeIds && 
                                // Simple hit test
                                offset.x >= (pos.x + viewportOffset.x) - 80f &&
                                offset.x <= (pos.x + viewportOffset.x) + 80f &&
                                offset.y >= (pos.y + viewportOffset.y) - 25f &&
                                offset.y <= (pos.y + viewportOffset.y) + 25f
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
            // Draw Background Grid
            val gridSize = 20.dp.toPx()
            val gridColor = Color(0xFFFFFFFF).copy(alpha = 0.03f)
            
            // Calculate grid offset based on viewport
            val offsetX = viewportOffset.x % gridSize
            val offsetY = viewportOffset.y % gridSize
            
            // Draw vertical lines
            for (x in 0..(size.width / gridSize).toInt() + 1) {
                val xPos = x * gridSize + offsetX
                // Adjust if off screen
                val actualX = if (xPos > size.width) xPos - gridSize else if (xPos < 0) xPos + gridSize else xPos
                 drawLine(
                    color = gridColor,
                    start = Offset(actualX, 0f),
                    end = Offset(actualX, size.height),
                    strokeWidth = 1f
                )
            }
            // Draw horizontal lines
            for (y in 0..(size.height / gridSize).toInt() + 1) {
                 val yPos = y * gridSize + offsetY
                 drawLine(
                    color = gridColor,
                    start = Offset(0f, yPos),
                    end = Offset(size.width, yPos),
                    strokeWidth = 1f
                )
            }

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
                            edgeColor = AccentPrimary.copy(alpha = 0.8f) // Outgoing - Accent
                            strokeWidth = 2f
                        } else if (edge.to == selectedNodeId) {
                            edgeColor = Color(0xFFE57373) // Incoming - Red? Or keep consistent? Let's use red for now
                            strokeWidth = 2f
                        } else {
                            edgeColor = Color(0xFF444444) // Default edges
                        }
                    } else {
                         edgeColor = Color(0xFF444444)
                    }
                    
                    // Draw Curved Edge
                    val path = Path()
                    path.moveTo(actualStart.x, actualStart.y)
                    
                    // Control points for bezier
                    // A simple curve strategy: pull perpendicular to the line connecting them? 
                    // Or just horizontal curvature if it was a tree. Since it's a graph, maybe just slight curvature.
                    val dx = actualEnd.x - actualStart.x
                    val dy = actualEnd.y - actualStart.y
                    
                    // We can use a quadratic bezier with control point offset
                    // Or cubic with control points near start and end
                    val cp1 = Offset(actualStart.x + dx * 0.4f, actualStart.y)
                    val cp2 = Offset(actualEnd.x - dx * 0.4f, actualEnd.y)
                    // If dx is small (vertical), we might want to curve differently.
                    // Let's just use simple cubic interpolation
                    
                    path.cubicTo(
                        actualStart.x + dx / 2, actualStart.y,
                        actualEnd.x - dx / 2, actualEnd.y,
                        actualEnd.x, actualEnd.y
                    )
                    
                    drawPath(
                        path = path,
                        color = edgeColor,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Draw tiny arrowhead at the end? (Optional but requested "minimal, thin")
                    // Would need rotation calculation. Skipping for now to keep simple, focusing on curve.
                    
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
            // Draw Nodes
            visibleNodes.forEach { node ->
                val pos = nodePositions[node.id] ?: return@forEach
                val actualPos = pos + viewportOffset
                
                var color = getNodeColor(node.type)
                var alpha = 1f
                
                // Search Highlighting
                val isSearchMatch = searchQuery.isNotEmpty() && node.name.contains(searchQuery, ignoreCase = true)
                if (searchQuery.isNotEmpty() && !isSearchMatch) {
                    alpha = 0.3f
                }
                
                // Node Dimensions
                val nodeWidth = 160f
                val nodeHeight = 60f // Increased for better padding
                
                // Draw drop shadow
                // (Shadow removed for Multiplatform compatibility)
                /*
                val shadowColor = Color.Black.copy(alpha = 0.35f)
                val paint = Paint()
                val frameworkPaint = paint.asFrameworkPaint()
                // frameworkPaint.setShadowLayer(...) // Not available on Desktop Skia
                
                drawIntoCanvas {
                   // ...
                }
                */
                
                // Draw rounded rect background
                drawRoundRect(
                    color = Color(0xFF1F1F1F).copy(alpha = alpha), // New Node Background
                    topLeft = actualPos - Offset(nodeWidth / 2, nodeHeight / 2),
                    size = Size(nodeWidth, nodeHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()), // 10px rad
                )
                
                // Draw left colored strip
                // Clip drawing to rounded rect for strip? Or just draw a rect with rounded corners on left only.
                // Easier: Draw grid then mask... no, simplest is just a smaller rounded rect or path.
                // Let's draw a vertical strip that overlaps slightly or is carefully placed.
                // Or just a separate rect on the left side
                
                val startX = actualPos.x - nodeWidth / 2
                val startY = actualPos.y - nodeHeight / 2
                
                // Left border strip (4px wide)
                // We need to respect the corner radius on top-left and bottom-left.
                // Using a path is best.
                val stripPath = Path().apply {
                    moveTo(startX + 4f, startY) // Top right of strip
                    lineTo(startX + 10f, startY) // Start curve
                    // Arc for top left
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(startX, startY, startX + 20f, startY + 20f),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    lineTo(startX, startY + nodeHeight - 10f)
                     arcTo(
                        rect = androidx.compose.ui.geometry.Rect(startX, startY + nodeHeight - 20f, startX + 20f, startY + nodeHeight),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = false
                    )
                    lineTo(startX + 4f, startY + nodeHeight)
                    close()
                }
                // Actually, that path is complex. Just drawing a thin rounded rect on the left might be enough visuals?
                // Spec: "Color-coded left border strip (thin, 4px)"
                // Let's simply draw a line or rect that is clipped or overlayed.
                
                // Simplified: Draw color badge on Text instead of heavy left strip if path is hard.
                // But spec says "Color-coded left border strip" AND "Node Type Badge".
                // Let's try the simple strip:
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = actualPos - Offset(nodeWidth / 2, nodeHeight / 2),
                    size = Size(6f, nodeHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()) // This will round all corners, looking like a pill on left.
                    // To make it look like a left border, we can draw a rect over the right side of it with background color? 
                    // No, let's just stick to the specific capsule badge requested in spec: "A small capsule on the top-right"
                    // AND "Color-coded left border strip". 
                )
                
                // Draw border
                var borderColor = Color(0xFF2A2A2A).copy(alpha = alpha) 
                var borderWidth = 1f
                
                if (selectedNodeId == node.id) {
                    borderColor = AccentPrimary
                    borderWidth = 2f
                } else if (isSearchMatch) {
                    borderColor = Color.Yellow
                    borderWidth = 2f
                }
                
                drawRoundRect(
                    color = borderColor,
                    topLeft = actualPos - Offset(nodeWidth / 2, nodeHeight / 2),
                    size = Size(nodeWidth, nodeHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(width = borderWidth)
                )

                // Type Badge (Top Right)
                val badgeText = node.type.name
                val badgeLayout = textMeasurer.measure(
                    text = badgeText,
                    style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = alpha))
                )
                // Draw badge background
                val badgePadding = 4.dp.toPx()
                val badgeWidth = badgeLayout.size.width + badgePadding * 2
                val badgeHeight = badgeLayout.size.height + badgePadding
                
                val badgeX = actualPos.x + nodeWidth / 2 - badgeWidth - 8f
                val badgeY = actualPos.y - nodeHeight / 2 + 8f
                
                drawRoundRect(
                    color = color.copy(alpha = 0.15f * alpha),
                    topLeft = Offset(badgeX, badgeY),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawText(
                    textLayoutResult = badgeLayout,
                    topLeft = Offset(badgeX + badgePadding, badgeY + badgePadding / 2)
                )
                
                // Title
                drawText(
                    textMeasurer,
                    text = node.name,
                    style = TextStyle(
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.SemiBold, 
                        color = Color(0xFFECECEC).copy(alpha = alpha)
                    ),
                    topLeft = actualPos - Offset(nodeWidth / 2 - 16f, nodeHeight / 2 - 20f)
                )
                
                // Subtitle (Generic or File info)
                drawText(
                    textMeasurer,
                    text = "Node", // Or generic subtitle
                    style = TextStyle(
                        fontSize = 11.sp, 
                        color = Color(0xFFA7A7A7).copy(alpha = alpha)
                    ),
                    topLeft = actualPos - Offset(nodeWidth / 2 - 16f, nodeHeight / 2 - 40f)
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
        NodeType.SENSOR_SOURCE -> Color(0xFFF77F00) // Sensor Node
        NodeType.LOGIC -> Color(0xFF7209B7)         // Logic -> Utility/Business?
        NodeType.VIEWMODEL -> Color(0xFF4361EE)     // ViewModel
        NodeType.UI -> Color(0xFF4CC9F0)            // UI -> Activity/View
        NodeType.GENERIC -> Color(0xFFA8A8A8)       // File
    }
}
