package com.github.drazumova.plugintest.dataset

import com.github.javaparser.ParseException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration

fun methodTextRange(fileText: String, method: String): Pair<Int, Int> {
    if (method.isEmpty()) return Pair(-1, -1)
    val methodName = method
        .dropLastWhile { !(it.isLetter() || it.isDigit() || (it == '_')) }
        .removeSuffix("$\$lambda$$")
        .takeLastWhile { it != '.' && it != '$'}
    try {
        val compilationUnit = StaticJavaParser.parse(fileText)
        val methodDeclaration = compilationUnit.findAll(MethodDeclaration::class.java).firstOrNull {
            it.nameAsString == methodName
        }
            ?: return Pair(-1, -1)
        return Pair(methodDeclaration.range.get().begin.line, methodDeclaration.range.get().end.line)
    } catch (x: ParseException) {
        println(x)
        println(x.message)
        println(x.localizedMessage)
        return Pair(-1, -1)
    }
}

fun Pair<Int, Int>.cross(other: Pair<Int, Int>): Boolean {
    return !(first > other.second || second < other.first)
}