package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MenuBar(
    onSaveAnalysis: () -> Unit,
    onSaveAnalysisAs: () -> Unit,
    onLoadAnalysis: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    enabled: Boolean = true
) {
    Row {
        // File Menu
        var fileMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { fileMenuExpanded = true }) {
                Text("File", color = Color.White)
            }
            DropdownMenu(
                expanded = fileMenuExpanded,
                onDismissRequest = { fileMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        onSaveAnalysis()
                        fileMenuExpanded = false
                    },
                    enabled = enabled
                ) {
                    Text("Save Analysis")
                }
                DropdownMenuItem(
                    onClick = {
                        onSaveAnalysisAs()
                        fileMenuExpanded = false
                    },
                    enabled = enabled
                ) {
                    Text("Save Analysis As...")
                }
                DropdownMenuItem(
                    onClick = {
                        onLoadAnalysis()
                        fileMenuExpanded = false
                    }
                ) {
                    Text("Load Analysis")
                }
                Divider()
                DropdownMenuItem(
                    onClick = {
                        onExit()
                        fileMenuExpanded = false
                    }
                ) {
                    Text("Exit")
                }
            }
        }

        // Connections Menu
        var connectionsMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { connectionsMenuExpanded = true }) {
                Text("Connections", color = Color.White)
            }
            DropdownMenu(
                expanded = connectionsMenuExpanded,
                onDismissRequest = { connectionsMenuExpanded = false }
            ) {
                DropdownMenuItem(onClick = { connectionsMenuExpanded = false }, enabled = false) {
                    Text("(Coming soon)")
                }
            }
        }

        // View Menu
        var viewMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { viewMenuExpanded = true }) {
                Text("View", color = Color.White)
            }
            DropdownMenu(
                expanded = viewMenuExpanded,
                onDismissRequest = { viewMenuExpanded = false }
            ) {
                DropdownMenuItem(onClick = { viewMenuExpanded = false }, enabled = false) {
                    Text("(Coming soon)")
                }
            }
        }

        // Edit Menu
        var editMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { editMenuExpanded = true }) {
                Text("Edit", color = Color.White)
            }
            DropdownMenu(
                expanded = editMenuExpanded,
                onDismissRequest = { editMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        onSettings()
                        editMenuExpanded = false
                    }
                ) {
                    Text("Settings")
                }
            }
        }

        // Help Menu
        var helpMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { helpMenuExpanded = true }) {
                Text("Help", color = Color.White)
            }
            DropdownMenu(
                expanded = helpMenuExpanded,
                onDismissRequest = { helpMenuExpanded = false }
            ) {
                DropdownMenuItem(onClick = { helpMenuExpanded = false }) {
                    Text("About SenseDev")
                }
                DropdownMenuItem(onClick = { helpMenuExpanded = false }) {
                    Text("Documentation")
                }
            }
        }
    }
}
