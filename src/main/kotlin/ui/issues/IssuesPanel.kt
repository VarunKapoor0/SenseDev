package ui.issues

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import core.issues.Issue
import core.issues.IssueType
import core.issues.Severity

@Composable
fun IssuesPanel(
    issues: List<Issue>,
    onIssueSelected: (Issue) -> Unit = {}
) {
    if (issues.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                "No issues detected ",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                style = MaterialTheme.typography.h6
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                "Detected Issues (${issues.size})",
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(issues) { issue ->
            IssueCard(issue, onIssueSelected)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun IssueCard(issue: Issue, onIssueSelected: (Issue) -> Unit) {
    Card(
        backgroundColor = Color(0xFF1E1E1E),
        elevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onIssueSelected(issue) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = getSeverityColor(issue.severity)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.type.name.replace("_", " "),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = issue.severity.name,
                    style = MaterialTheme.typography.caption,
                    color = getSeverityColor(issue.severity),
                    modifier = Modifier
                        .background(getSeverityColor(issue.severity).copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = issue.description,
                style = MaterialTheme.typography.body2,
                color = Color(0xFFDDDDDD)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Recommendation: ${issue.recommendation}",
                style = MaterialTheme.typography.caption,
                color = Color(0xFFBBBBBB)
            )
        }
    }
}

fun getSeverityColor(severity: Severity): Color {
    return when (severity) {
        Severity.HIGH -> Color(0xFFCF6679) // Red/Pink
        Severity.MEDIUM -> Color(0xFFFFB74D) // Orange
        Severity.LOW -> Color(0xFF81C784) // Green
    }
}
