package com.github.drazumova.plugintest.dataset

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.InvalidPathException


data class Features(
    val lineNumber: Int,
    val isFirstLine: Int,
    val isLastModified: Int,
    val editable: Int
)

internal fun Features.toList(reportId: String): List<String> {
    return listOf(
        reportId,
        lineNumber.toString(),
        isFirstLine.toString(),
        isLastModified.toString(),
        editable.toString()
    )
}

data class FullStacktraceLineInformation(
    val lineNumber: Int,
    val filename: String,
    val method: String,
    val annotation: AnnotatedFile?,
    val fileText: String
)

fun Boolean.toInt() = if (this) 1 else 0

fun features(lines: List<FullStacktraceLineInformation>): List<Features> {
    val methodModificationTimes = lines.map {
        val methodRange = methodTextRange(it.fileText, it.method)
//        println("${it.fileText} $methodRange")
        val methodModificationTime =
            if (methodRange.first != -1)
                it.annotation?.lineAnnotation
                    ?.slice(IntRange(methodRange.first, methodRange.second))
                    ?.map { line -> line.time }
                    ?.maxOrNull() ?: -1
            else
                -1
        methodModificationTime
    }
    val firstEditableLine = lines.firstOrNull { it.annotation != null }

    return lines.mapIndexed { index: Int, line: FullStacktraceLineInformation ->
        Features(
            index,
            (line == firstEditableLine).toInt(),
            (methodModificationTimes[index] == methodModificationTimes.maxOrNull()).toInt(),
            (line.annotation != null).toInt()
        )
    }
}

fun lineFromReportDirectory(directory: File): List<Features>? {
    val files = directory.listFiles() ?: return null
    val reportFile = files.singleOrNull { file -> file.extension == "json" } ?: return null
    val filesDirectory = files.singleOrNull { file -> file.isDirectory } ?: return null

    val report = Json.decodeFromString<Report>(reportFile.readText())
    val lines = report.lines.map {
        val reportDirectory = File(filesDirectory, it.filename)
        val reportLineFile = File(File(reportDirectory, "files"), it.filename)
        if (!reportDirectory.exists() || !reportLineFile.exists())
            return@map FullStacktraceLineInformation(
                it.line, it.filename, it.methodName, null, ""
            )

        val fileText = reportLineFile.readText()

        val savedAnnotation = File(filesDirectory, "${it.filename}.json")
        val annotatedFile = Json.decodeFromString<AnnotatedFile?>(savedAnnotation.readText())

        FullStacktraceLineInformation(it.line, it.filename, it.methodName, annotatedFile, fileText)
    }

    return features(lines)
}

fun main(args: Array<String>) {
    val inputPath = args[0]
    val outputFile = args[1]

    if (!File(outputFile).exists()) File(outputFile).createNewFile()

    val directory = File(inputPath)

    if (!directory.exists() || !directory.isDirectory)
        throw InvalidPathException(inputPath, "Empty or broken files directory")

    val features = directory.listFiles()?.flatMap { file ->
        val reportLines = lineFromReportDirectory(file) ?: emptyList()
        println(file.name)
        reportLines.map { it.toList(file.name) }
    } ?: emptyList()


    val columns = Features::class.java.declaredFields.map { it.name }

    csvWriter().open("test_2.csv") {
        writeRow(listOf("reportId").plus(columns))
        writeRows(features)
    }
}