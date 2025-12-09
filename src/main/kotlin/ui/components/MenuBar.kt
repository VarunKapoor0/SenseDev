package ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import core.ai.AISettings
import ui.theme.*

@Composable
fun MenuBar(
    onSaveAnalysis: () -> Unit,
    onSaveAnalysisAs: () -> Unit,
    onLoadAnalysis: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
    settingsVersion: Int = 0,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF1A1A1A)) // Toolbar background
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon/Name?
        // Text("SenseDev", style = MaterialTheme.typography.subtitle1, color = Color.White, modifier = Modifier.padding(end = 16.dp))

        // File Menu
        var fileMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { fileMenuExpanded = true }) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("File", color = TextPrimary)
            }
            DropdownMenu(
                expanded = fileMenuExpanded,
                onDismissRequest = { fileMenuExpanded = false },
                modifier = Modifier.background(PanelBackground)
            ) {
                DropdownMenuItem(
                    onClick = {
                        onSaveAnalysis()
                        fileMenuExpanded = false
                    },
                    enabled = enabled
                ) {
                    Text("Save Analysis", color = TextPrimary)
                }
                DropdownMenuItem(
                    onClick = {
                        onSaveAnalysisAs()
                        fileMenuExpanded = false
                    },
                    enabled = enabled
                ) {
                    Text("Save Analysis As...", color = TextPrimary)
                }
                DropdownMenuItem(
                    onClick = {
                        onLoadAnalysis()
                        fileMenuExpanded = false
                    }
                ) {
                    Text("Load Analysis", color = TextPrimary)
                }
                Divider(color = DividerColor)
                DropdownMenuItem(
                    onClick = {
                        onExit()
                        fileMenuExpanded = false
                    }
                ) {
                    Text("Exit", color = TextPrimary)
                }
            }
        }

        // Edit Menu
        var editMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { editMenuExpanded = true }) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", color = TextPrimary)
            }
            DropdownMenu(
                expanded = editMenuExpanded,
                onDismissRequest = { editMenuExpanded = false },
                modifier = Modifier.background(PanelBackground)
            ) {
                DropdownMenuItem(
                    onClick = {
                        onSettings()
                        editMenuExpanded = false
                    }
                ) {
                    Text("Settings", color = TextPrimary)
                }
            }
        }

        // View Menu
        var viewMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { viewMenuExpanded = true }) {
                Icon(Icons.Default.ViewQuilt, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("View", color = TextPrimary)
            }
            DropdownMenu(
                expanded = viewMenuExpanded,
                onDismissRequest = { viewMenuExpanded = false },
                modifier = Modifier.background(PanelBackground)
            ) {
                DropdownMenuItem(onClick = { viewMenuExpanded = false }, enabled = false) {
                    Text("(Coming soon)", color = TextDisabled)
                }
            }
        }
        
        // Help Menu
        var helpMenuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { helpMenuExpanded = true }) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Help", color = TextPrimary)
            }
            DropdownMenu(
                expanded = helpMenuExpanded,
                onDismissRequest = { helpMenuExpanded = false },
                modifier = Modifier.background(PanelBackground)
            ) {
                DropdownMenuItem(onClick = { helpMenuExpanded = false }) {
                    Text("About SenseDev", color = TextPrimary)
                }
                DropdownMenuItem(onClick = { helpMenuExpanded = false }) {
                    Text("Documentation", color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Model Indicator
        Surface(
            color = Color(0xFF252525),
            shape = RoundedCornerShape(12.dp),
            contentColor = AccentMuted,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            val selectedModelName by remember(settingsVersion) { 
                mutableStateOf(AISettings.selectedModel.ifBlank { "Local" }) 
            }
            
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp), tint = AccentMuted)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = selectedModelName,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}
