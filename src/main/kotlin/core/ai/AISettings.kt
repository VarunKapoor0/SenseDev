package core.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AISettingsData(
    var selectedProvider: AIProviderType = AIProviderType.LOCAL,
    var geminiApiKey: String = "",
    var openaiApiKey: String = "",
    var codeAccessLevel: CodeAccessLevel = CodeAccessLevel.SNIPPET_ONLY,
    var maxCodeTokens: Int = 2000,
    var selectedModel: String = "gemini-2.5-flash"
)

object AISettings {
    private val settingsFile = File(System.getProperty("user.home"), ".sensedev/ai-settings.json")
    private var data = AISettingsData()
    
    init {
        loadSettings()
    }
    
    var selectedProvider: AIProviderType
        get() = data.selectedProvider
        set(value) {
            data.selectedProvider = value
            saveSettings()
        }
    
    var geminiApiKey: String
        get() = data.geminiApiKey
        set(value) {
            data.geminiApiKey = value
            saveSettings()
        }
    
    var openaiApiKey: String
        get() = data.openaiApiKey
        set(value) {
            data.openaiApiKey = value
            saveSettings()
        }
    
    var codeAccessLevel: CodeAccessLevel
        get() = data.codeAccessLevel
        set(value) {
            data.codeAccessLevel = value
            saveSettings()
        }
    
    var maxCodeTokens: Int
        get() = data.maxCodeTokens
        set(value) {
            data.maxCodeTokens = value
            saveSettings()
        }
    
    var selectedModel: String
        get() = data.selectedModel
        set(value) {
            data.selectedModel = value
            saveSettings()
        }
    
    fun saveSettings() {
        try {
            settingsFile.parentFile?.mkdirs()
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(data)
            settingsFile.writeText(jsonString)
        } catch (e: Exception) {
            println("Failed to save AI settings: ${e.message}")
        }
    }
    
    fun loadSettings() {
        try {
            if (settingsFile.exists()) {
                val jsonString = settingsFile.readText()
                val json = Json { ignoreUnknownKeys = true }
                data = json.decodeFromString(jsonString)
            }
        } catch (e: Exception) {
            println("Failed to load AI settings: ${e.message}")
        }
    }
}
