package ui.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun CodePanel(
    filePath: String?,
    lineNumber: Int?,
    modifier: Modifier = Modifier
) {
    if (filePath == null) {
        Box(modifier = modifier.fillMaxSize()) {
            Text(
                "No file selected",
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                color = Color.White
            )
        }
        return
    }

    val file = File(filePath)
    if (!file.exists()) {
        Box(modifier = modifier.fillMaxSize()) {
            Text(
                "File not found: $filePath",
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                color = MaterialTheme.colors.error
            )
        }
        return
    }

    val lines = remember(filePath) { file.readLines() }
    val scrollState = rememberScrollState()

    // Auto-scroll to line
    LaunchedEffect(lineNumber) {
        if (lineNumber != null && lineNumber > 0 && lineNumber <= lines.size) {
            // Estimate scroll position (simple approximation)
            // A better approach would be using LazyColumn, but for now simple scroll is okay
            // or we can try to calculate offset. 
            // For simplicity with Column + verticalScroll, we might just scroll to a ratio
            // But accurate scrolling requires item height knowledge.
            // Let's stick to simple display first, maybe scroll later if needed or use LazyColumn
            scrollState.scrollTo(lineNumber * 50) // Rough guess
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        SelectionContainer {
            Column {
                lines.forEachIndexed { index, line ->
                    val currentLineNumber = index + 1
                    val isTargetLine = currentLineNumber == lineNumber
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isTargetLine) Color(0xFF3E3E3E) else Color.Transparent)
                    ) {
                        // Line Number
                        Text(
                            text = "$currentLineNumber",
                            color = Color.Gray,
                            modifier = Modifier.width(40.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        
                        // Code Content
                        Text(
                            text = line,
                            color = Color(0xFFD4D4D4),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
