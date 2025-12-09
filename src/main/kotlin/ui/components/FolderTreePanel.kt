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
import java.io.File

data class FileTreeNode(
    val file: File,
    val children: List<FileTreeNode> = emptyList(),
    val isExpanded: MutableState<Boolean> = mutableStateOf(false)
)

@Composable
fun FolderTreePanel(
    rootPath: String,
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
                "Project Files",
                style = MaterialTheme.typography.subtitle1,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(listOf(fileTree)) { node ->
            FileTreeItem(node, depth = 0, onFileSelected = onFileSelected)
        }
    }
}

@Composable
fun FileTreeItem(
    node: FileTreeNode,
    depth: Int,
    onFileSelected: (File) -> Unit
) {
    val isExpanded = node.isExpanded.value

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.file.isDirectory) {
                        node.isExpanded.value = !isExpanded
                    } else {
                        onFileSelected(node.file)
                    }
                }
                .padding(start = (depth * 16).dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expansion icon for directories
            if (node.file.isDirectory) {
                Text(
                    text = if (isExpanded) "▼ " else "▶ ",
                    color = Color.White,
                    modifier = Modifier.width(20.dp)
                )
                Text(
                    text = "📁 ",
                    color = Color(0xFFBB86FC)
                )
            } else {
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "📄 ",
                    color = Color(0xFF03DAC6)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = node.file.name,
                style = MaterialTheme.typography.body2,
                color = Color.White
            )
        }

        // Show children if expanded
        if (isExpanded && node.file.isDirectory) {
            node.children.forEach { child ->
                FileTreeItem(child, depth + 1, onFileSelected)
            }
        }
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
