package core.model

import kotlinx.serialization.Serializable

/**
 * Represents a class symbol extracted from source code
 */
@Serializable
data class ClassSymbol(
    val name: String,
    val qualifiedName: String,
    val filePath: String,
    val packageName: String,
    val superClass: String? = null,
    val interfaces: List<String> = emptyList(),
    val methods: List<MethodSymbol> = emptyList(),
    val fields: List<FieldSymbol> = emptyList(),
    val isActivity: Boolean = false,
    val isFragment: Boolean = false,
    val isViewModel: Boolean = false,
    val isComposable: Boolean = false
)

/**
 * Represents a method/function symbol
 */
@Serializable
data class MethodSymbol(
    val name: String,
    val qualifiedName: String,
    val returnType: String,
    val parameters: List<Parameter> = emptyList(),
    val callsTo: List<String> = emptyList(), // Qualified names of called methods
    val lineNumber: Int = 0,
    val annotations: List<String> = emptyList()
)

/**
 * Represents a field/property symbol
 */
@Serializable
data class FieldSymbol(
    val name: String,
    val type: String,
    val isLiveData: Boolean = false,
    val isStateFlow: Boolean = false,
    val isFlow: Boolean = false,
    val lineNumber: Int = 0
)

/**
 * Represents a method parameter
 */
@Serializable
data class Parameter(
    val name: String,
    val type: String
)

/**
 * Project-wide symbol map
 */
@Serializable
data class SymbolMap(
    val classes: Map<String, ClassSymbol> = emptyMap(), // Qualified name -> ClassSymbol
    val files: Map<String, List<String>> = emptyMap()   // File path -> List of class qualified names
)
