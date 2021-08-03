package com.github.drazumova.plugintest.dataset

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.*
import kotlinx.ast.common.klass.KlassDeclaration
import kotlinx.ast.grammar.kotlin.common.KotlinGrammarParserType
import kotlinx.ast.grammar.kotlin.common.summary

import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser

private fun javaMethodRange(fileText: String, methodName: String): Pair<Int, Int> {
    val compilationUnit = StaticJavaParser.parse(fileText)
    val methodDeclaration = compilationUnit.findAll(MethodDeclaration::class.java).firstOrNull {
        it.nameAsString == methodName
    }
        ?: return Pair(-1, -1)
    return Pair(methodDeclaration.range.get().begin.line, methodDeclaration.range.get().end.line)
}


private class MethodCollector(private val methodName: String) {
    var begin = -1
    var end = -1

    val range: Pair<Int, Int>
        get() = Pair(begin, end)

    fun checkKlass(ast: KlassDeclaration) {
        if (ast.identifier?.rawName == methodName) {
            begin = ast.info?.start?.line ?: -1
            end = ast.info?.stop?.line ?: -1
        }
    }

    fun checkMethod(ast: MethodDeclaration) {
        if (ast.nameAsString == methodName) {
            begin = ast.begin.get().line
            end = ast.begin.get().line
        }
    }
}

private fun visitKotlinFile(list: List<Ast>, collector: MethodCollector) {
    for (ast in list) {
        if (ast is KlassDeclaration) {
            collector.checkKlass(ast)
        }
        if (ast is MethodDeclaration) {
            collector.checkMethod(ast)
        }
        if (ast is AstNode) {
            visitKotlinFile(ast.children, collector)
        }
    }
}

private fun kotlinMethodRange(fileText: String, methodName: String): Pair<Int, Int> {
    val kotlinFile = KotlinGrammarAntlrKotlinParser.parse(AstSource.String("source code", fileText),
            KotlinGrammarParserType.kotlinFile)

    val methodInfoCollector = MethodCollector(methodName)
    kotlinFile.summary(false).onSuccess {
        visitKotlinFile(it, methodInfoCollector)
    }
    return methodInfoCollector.range
}

fun methodTextRange(fileText: String, method: String): Pair<Int, Int> {
    if (method.isEmpty()) return Pair(-1, -1)
    val methodName = method
        .dropLastWhile { !(it.isLetter() || it.isDigit() || (it == '_')) }
        .removeSuffix("$\$lambda$$")
        .takeLastWhile { it != '.' && it != '$'}

    try {
        return javaMethodRange(fileText, methodName)
    } catch (x: com.github.javaparser.ParseProblemException) {
//        println(x)
//        println(x.message)
//        println(x.localizedMessage)
    }

    return kotlinMethodRange(fileText, methodName)
}

fun Pair<Int, Int>.cross(other: Pair<Int, Int>): Boolean {
    return !(first > other.second || second < other.first)
}
