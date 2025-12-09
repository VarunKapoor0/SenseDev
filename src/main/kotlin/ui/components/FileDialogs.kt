package ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun SaveFileDialog(
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Save Analysis As"
        fileChooser.fileFilter = FileNameExtensionFilter("JSON Files (*.json)", "json")
        fileChooser.selectedFile = java.io.File("analysis.json")
        
        val result = fileChooser.showSaveDialog(null)
        
        if (result == JFileChooser.APPROVE_OPTION) {
            var filePath = fileChooser.selectedFile.absolutePath
            // Add .json extension if not present
            if (!filePath.endsWith(".json", ignoreCase = true)) {
                filePath += ".json"
            }
            onFileSelected(filePath)
        } else {
            onDismiss()
        }
    }
}

@Composable
fun LoadFileDialog(
    onFileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Load Analysis"
        fileChooser.fileFilter = FileNameExtensionFilter("JSON Files (*.json)", "json")
        
        val result = fileChooser.showOpenDialog(null)
        
        if (result == JFileChooser.APPROVE_OPTION) {
            val filePath = fileChooser.selectedFile.absolutePath
            onFileSelected(filePath)
        } else {
            onDismiss()
        }
    }
}
