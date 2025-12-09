package ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import core.graph.NodeType

@Composable
fun GraphToolbar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    activeNodeFilters: Set<NodeType>,
    onNodeFilterChanged: (Set<NodeType>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilters by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().background(Color(0xFF2D2D2D))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                placeholder = { Text("Search nodes...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color(0xFF1E1E1E),
                    textColor = Color.White,
                    cursorColor = MaterialTheme.colors.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Filter Toggle
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    Icons.Default.List,
                    contentDescription = "Filters",
                    tint = if (showFilters || activeNodeFilters.isNotEmpty()) MaterialTheme.colors.primary else Color.Gray
                )
            }
        }

        // Filter Options
        if (showFilters) {
            Divider(color = Color(0xFF3E3E3E))
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Node Types:", color = Color.Gray, style = MaterialTheme.typography.caption)
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NodeType.values().forEach { type ->
                        FilterChip(
                            text = type.name,
                            selected = type in activeNodeFilters,
                            onClick = {
                                val newFilters = activeNodeFilters.toMutableSet()
                                if (type in newFilters) {
                                    newFilters.remove(type)
                                } else {
                                    newFilters.add(type)
                                }
                                onNodeFilterChanged(newFilters)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colors.primary else Color(0xFF3E3E3E),
        contentColor = if (selected) Color.Black else Color.White,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption
        )
    }
}
