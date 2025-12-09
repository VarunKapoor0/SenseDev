package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import java.io.File
import ui.theme.*

data class FileTreeNode(
    val file: File,
    val children: List<FileTreeNode> = emptyList(),
    val isExpanded: MutableState<Boolean> = mutableStateOf(false)
)

@Composable
fun FolderTreePanel(
    rootPath: String,
    selectedFile: File? = null,
    onFileSelected: (File) -> Unit = {}
) {
    val rootFile = File(rootPath)
    val fileTree = remember(rootPath) { buildFileTree(rootFile) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        item {
            Text(
                "PROJECT FILES",
                style = MaterialTheme.typography.overline,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
        }
        items(listOf(fileTree)) { node ->
            FileTreeItem(node, depth = 0, selectedFile = selectedFile, onFileSelected = onFileSelected)
        }
    }
}

@Composable
fun FileTreeItem(
    node: FileTreeNode,
    depth: Int,
    selectedFile: File?,
    onFileSelected: (File) -> Unit
) {
    val isExpanded = node.isExpanded.value
    val isSelected = selectedFile?.absolutePath == node.file.absolutePath

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) SurfaceHighlight else Color.Transparent)
                .clickable {
                    if (node.file.isDirectory) {
                        node.isExpanded.value = !isExpanded
                    } else {
                        onFileSelected(node.file)
                    }
                }
                .padding(start = (depth * 16).dp + 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expansion icon for directories
            if (node.file.isDirectory) {
                Text(
                    text = if (isExpanded) "⌄" else "›",
                    color = TextSecondary,
                    modifier = Modifier.width(16.dp),
                    style = MaterialTheme.typography.caption
                )
                // Minimal folder icon? Or just color.
                // Let's use a unicode folder or just color the text.
                // Doc says: "Folder icons: chevrons, minimal"
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Icon
            val iconColor = if (node.file.isDirectory) AccentMuted else getFileIconColor(node.file.name)
            
            // File/Folder Name
            Text(
                text = node.file.name,
                style = if (isSelected) MaterialTheme.typography.body2.copy(color = AccentPrimary) else MaterialTheme.typography.body2,
                color = if (isSelected) AccentPrimary else TextPrimary
            )
        }

        // Show children if expanded
        if (isExpanded && node.file.isDirectory) {
            node.children.forEach { child ->
                FileTreeItem(child, depth + 1, selectedFile, onFileSelected)
            }
        }
    }
}

private fun getFileIconColor(name: String): Color {
    return when {
        name.endsWith(".kt") -> Color(0xFF81C784) // Kotlin Green-ish
        name.endsWith(".xml") -> Color(0xFFE57373) // XML Red-ish
        name.endsWith(".gradle") || name.endsWith(".kts") -> Color.Gray
        else -> TextSecondary
    }
}

private fun buildFileTree(file: File): FileTreeNode {
    if (!file.isDirectory) {
        return FileTreeNode(file)
    }

    val children = file.listFiles()
        ?.filter { !it.name.startsWith(".") && it.name != "build" }
        ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
        ?.map { buildFileTree(it) }
        ?: emptyList()

    return FileTreeNode(file, children)
}
