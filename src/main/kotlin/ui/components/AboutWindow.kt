package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ui.theme.*

@Composable
fun AboutWindow(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    // Using Dialog for a modal experience
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = PanelBackground, // Use app theme background
            elevation = 8.dp,
            modifier = Modifier.width(400.dp) // Fixed width as requested
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header / App Name
                Text(
                    text = "SenseDev",
                    style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Version
                Text(
                    text = "Version 0.1.0-dev",
                    style = MaterialTheme.typography.body2,
                    color = TextSecondary
                )
                Text(
                    text = "Developer Preview",
                    style = MaterialTheme.typography.caption,
                    color = AccentMuted
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Divider(color = DividerColor)
                
                Spacer(modifier = Modifier.height(24.dp))

                // Description
                Text(
                    text = "Visual codebase exploration and understanding tool for developers.",
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Author",
                            style = MaterialTheme.typography.caption,
                            color = TextSecondary
                        )
                        Text(
                            text = "Varun Kapoor",
                            style = MaterialTheme.typography.body2,
                            color = TextPrimary
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Platform",
                            style = MaterialTheme.typography.caption,
                            color = TextSecondary
                        )
                        Text(
                            text = "Windows",
                            style = MaterialTheme.typography.body2,
                            color = TextPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Links
                TextButton(
                    onClick = { 
                        uriHandler.openUri("https://github.com/VarunKapoor0/SenseDev/blob/main/README.md")
                    }
                ) {
                    Text("View on GitHub", color = AccentPrimary)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3E3E3E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = TextPrimary)
                }
            }
        }
    }
}
