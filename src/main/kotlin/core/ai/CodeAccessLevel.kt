package core.ai

enum class CodeAccessLevel {
    SNIPPET_ONLY,    // Default: just the selected method/class
    FLOW_LEVEL,      // Multi-file: include flow-connected nodes
    FULL_FILE        // Explicit opt-in: entire file content
}
