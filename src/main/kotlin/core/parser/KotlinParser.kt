package core.parser

import core.model.*
import java.io.File

/**
 * Simplified parser for Kotlin files
 * NOTE: Full PSI parsing with Kotlin compiler is complex.
 * This is a simplified regex-based approach for MVP.
 */
class KotlinParser {
    
    fun parseFile(filePath: String): List<ClassSymbol> {
        val file = File(filePath)
        if (!file.exists()) return emptyList()
        
        val content = file.readText()
        val classes = mutableListOf<ClassSymbol>()
        
        // Extract package
        val packageName = extractPackage(content)
        
        // Find class declarations
        // Updated pattern to capture constructor parameters in group 2
        val classPattern = """(?:class|object|interface)\s+(\w+)(?:<[^>]+>)?(?:\s*\(([^)]*)\))?(?:\s*:\s*([^{]+))?\s*\{""".toRegex()
        val matches = classPattern.findAll(content)
        
        for (match in matches) {
            val className = match.groupValues[1]
            val constructorParams = match.groupValues[2]
            val inheritance = match.groupValues[3].trim()
            
            val qualifiedName = if (packageName.isNotEmpty()) {
                "$packageName.$className"
            } else {
                className
            }
            
            val superClass = extractSuperClass(inheritance)
            val interfaces = extractInterfaces(inheritance)
            
            // Detect Android component types
            val isActivity = superClass?.contains("Activity") == true
            val isFragment = superClass?.contains("Fragment") == true
            val isViewModel = superClass?.contains("ViewModel") == true
            val isComposable = content.contains("@Composable") && content.contains("fun $className")
            
            // Extract methods (simplified)
            val methods = extractMethods(content, qualifiedName)
            
            // Extract fields (simplified)
            val fields = extractFields(content).toMutableList()
            
            // Add primary constructor properties to fields
            if (constructorParams.isNotEmpty()) {
                val constructorFields = extractConstructorFields(constructorParams)
                fields.addAll(constructorFields)
            }
            
            classes.add(
                ClassSymbol(
                    name = className,
                    qualifiedName = qualifiedName,
                    filePath = filePath,
                    packageName = packageName,
                    superClass = superClass,
                    interfaces = interfaces,
                    methods = methods,
                    fields = fields,
                    isActivity = isActivity,
                    isFragment = isFragment,
                    isViewModel = isViewModel,
                    isComposable = isComposable
                )
            )
        }
        
        return classes
    }
    
    private fun extractPackage(content: String): String {
        val packagePattern = """package\s+([\w.]+)""".toRegex()
        return packagePattern.find(content)?.groupValues?.get(1) ?: ""
    }
    
    private fun extractSuperClass(inheritance: String): String? {
        if (inheritance.isEmpty()) return null
        val parts = inheritance.split(",")
        return parts.firstOrNull()?.trim()?.substringBefore("(")?.trim()
    }
    
    private fun extractInterfaces(inheritance: String): List<String> {
        if (inheritance.isEmpty()) return emptyList()
        val parts = inheritance.split(",")
        return parts.drop(1).map { it.trim().substringBefore("(").trim() }
    }
    
    private fun extractMethods(content: String, className: String): List<MethodSymbol> {
        val methods = mutableListOf<MethodSymbol>()
        
        // Match function declarations with their bodies
        val functionPattern = """fun\s+(\w+)\s*\([^)]*\)(?:\s*:\s*(\w+))?\s*\{""".toRegex()
        val matches = functionPattern.findAll(content)
        
        var lineNumber = 1
        for (match in matches) {
            val methodName = match.groupValues[1]
            val returnType = match.groupValues.getOrNull(2) ?: "Unit"
            val qualifiedMethodName = "$className.$methodName"
            
            // Check for annotations
            val annotations = mutableListOf<String>()
            val beforeMatch = content.substring(0, match.range.first)
            if (beforeMatch.takeLast(100).contains("@Composable")) {
                annotations.add("Composable")
            }
            
            // Extract method body to find calls
            val bodyStart = match.range.last + 1
            val bodyEnd = findMatchingBrace(content, bodyStart)
            val methodBody = if (bodyEnd > bodyStart) {
                content.substring(bodyStart, bodyEnd)
            } else {
                ""
            }
            
            // Extract method calls from body
            val calls = extractMethodCalls(methodBody, content)
            
            methods.add(
                MethodSymbol(
                    name = methodName,
                    qualifiedName = qualifiedMethodName,
                    returnType = returnType,
                    parameters = emptyList(), // Simplified for now
                    callsTo = calls,
                    lineNumber = lineNumber,
                    annotations = annotations
                )
            )
            
            lineNumber++
        }
        
        return methods
    }
    
    /**
     * Extract method calls from a method body
     */
    private fun extractMethodCalls(methodBody: String, fullContent: String): List<String> {
        val calls = mutableListOf<String>()
        
        // Pattern 1: object.method() or variable.method()
        // Capture receiver to help with resolution later
        // Updated to handle trailing lambdas: method { }
        val methodCallPattern = """(\w+)\.(\w+)\s*(?:\(|\{)""".toRegex()
        methodCallPattern.findAll(methodBody).forEach { match ->
            val receiver = match.groupValues[1]
            val method = match.groupValues[2]
            
            // Check common sensor APIs
            when {
                receiver == "SensorManager" || receiver.contains("sensorManager", ignoreCase = true) -> {
                    calls.add("android.hardware.SensorManager.$method")
                }
                receiver == "LocationManager" || receiver.contains("locationManager", ignoreCase = true) -> {
                    calls.add("android.location.LocationManager.$method")
                }
                method.contains("registerListener", ignoreCase = true) -> {
                    calls.add("$receiver.$method")
                }
                else -> {
                    // Store as "receiver.method" - GraphBuilder will resolve receiver type
                    calls.add("$receiver.$method")
                }
            }
        }
        
        // Pattern 2: Direct function calls (no receiver) - implicit 'this'
        val directCallPattern = """(?:^|\s)(\w+)\s*(?:\(|\{)""".toRegex()
        directCallPattern.findAll(methodBody).forEach { match ->
            val functionName = match.groupValues[1]
            // Skip common keywords
            if (functionName !in listOf("if", "when", "while", "for", "return", "val", "var", "fun", "class", "catch")) {
                calls.add(functionName)
            }
        }
        
        return calls.distinct()
    }
    
    /**
     * Find matching closing brace for a function body
     */
    private fun findMatchingBrace(content: String, start: Int): Int {
        var depth = 1
        var i = start
        while (i < content.length && depth > 0) {
            when (content[i]) {
                '{' -> depth++
                '}' -> depth--
            }
            i++
        }
        return if (depth == 0) i - 1 else start
    }
    
    private fun extractFields(content: String): List<FieldSymbol> {
        val fields = mutableListOf<FieldSymbol>()
        
        // Match val/var declarations
        // Improved regex to capture type better, handling generics like LiveData<Type>
        val fieldPattern = """(?:val|var)\s+(\w+)\s*:\s*([\w<>?.,\s]+?)(?:\s*=|$)""".toRegex(RegexOption.MULTILINE)
        val matches = fieldPattern.findAll(content)
        
        for (match in matches) {
            val fieldName = match.groupValues[1]
            val fieldType = match.groupValues[2].trim()
            
            fields.add(
                FieldSymbol(
                    name = fieldName,
                    type = fieldType,
                    isLiveData = fieldType.contains("LiveData"),
                    isStateFlow = fieldType.contains("StateFlow"),
                    isFlow = fieldType.contains("Flow")
                )
            )
        }
        
        return fields
    }
    
    private fun extractConstructorFields(params: String): List<FieldSymbol> {
        val fields = mutableListOf<FieldSymbol>()
        
        // Split by comma, but be careful about generics
        // Simple split for MVP, assuming no complex nested generics with commas in constructor
        val parts = params.split(",")
        
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("val") || trimmed.startsWith("var") || 
                trimmed.contains("val ") || trimmed.contains("var ")) {
                
                // Extract name and type
                // Pattern: (private/public/override) val/var name: Type
                val nameMatch = """(?:val|var)\s+(\w+)\s*:\s*([\w<>?]+)""".toRegex().find(trimmed)
                if (nameMatch != null) {
                    val name = nameMatch.groupValues[1]
                    val type = nameMatch.groupValues[2]
                    
                    fields.add(
                        FieldSymbol(
                            name = name,
                            type = type,
                            isLiveData = type.contains("LiveData"),
                            isStateFlow = type.contains("StateFlow"),
                            isFlow = type.contains("Flow")
                        )
                    )
                }
            }
        }
        
        return fields
    }
}
