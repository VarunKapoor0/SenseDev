package core.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import core.model.*
import java.io.File

/**
 * Parser for Java files using JavaParser library
 */
class JavaParserAdapter {
    
    private val javaParser = JavaParser()
    
    fun parseFile(filePath: String): List<ClassSymbol> {
        val file = File(filePath)
        if (!file.exists()) return emptyList()
        
        val parseResult = javaParser.parse(file)
        if (!parseResult.isSuccessful) {
            println("Failed to parse $filePath")
            return emptyList()
        }
        
        val cu = parseResult.result.orElse(null) ?: return emptyList()
        
        return extractClasses(cu, filePath)
    }
    
    private fun extractClasses(cu: CompilationUnit, filePath: String): List<ClassSymbol> {
        val classes = mutableListOf<ClassSymbol>()
        val packageName = cu.packageDeclaration.map { it.nameAsString }.orElse("")
        
        cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
            val className = classDecl.nameAsString
            val qualifiedName = if (packageName.isNotEmpty()) {
                "$packageName.$className"
            } else {
                className
            }
            
            val superClass = classDecl.extendedTypes.firstOrNull()?.nameAsString
            val interfaces = classDecl.implementedTypes.map { it.nameAsString }
            
            // Detect Android component types
            val isActivity = superClass?.contains("Activity") == true
            val isFragment = superClass?.contains("Fragment") == true
            val isViewModel = superClass?.contains("ViewModel") == true
            
            // Extract methods
            val methods = classDecl.methods.map { methodDecl ->
                MethodSymbol(
                    name = methodDecl.nameAsString,
                    qualifiedName = "$qualifiedName.${methodDecl.nameAsString}",
                    returnType = methodDecl.typeAsString,
                    parameters = methodDecl.parameters.map { param ->
                        Parameter(param.nameAsString, param.typeAsString)
                    },
                    lineNumber = methodDecl.begin.map { it.line }.orElse(0)
                )
            }
            
            // Extract fields
            val fields = classDecl.fields.flatMap { fieldDecl ->
                fieldDecl.variables.map { variable ->
                    FieldSymbol(
                        name = variable.nameAsString,
                        type = variable.typeAsString,
                        isLiveData = variable.typeAsString.contains("LiveData"),
                        lineNumber = fieldDecl.begin.map { it.line }.orElse(0)
                    )
                }
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
                    isViewModel = isViewModel
                )
            )
        }
        
        return classes
    }
}
