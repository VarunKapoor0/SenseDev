import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import controller.ProjectController
import state.AppState
import ui.App

fun main() = application {
    val appState = AppState()
    val projectController = ProjectController(appState)

    Window(
        onCloseRequest = ::exitApplication,
        title = "SenseDev",
        state = rememberWindowState(width = 1280.dp, height = 800.dp)
    ) {
        window.minimumSize = java.awt.Dimension(800, 600)
        App(appState, projectController)
    }
}
