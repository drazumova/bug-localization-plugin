package com.github.drazumova.plugintest.dataset

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.idea.configuration.fetchGradleVersion
import java.io.File
import java.nio.file.InvalidPathException


data class Features(
    val lineNumber: Int,
    val isFirstLine: Int,
    val isLastModified: Int,
    val editable: Int
)

internal fun Features.toList(reportId: String, label: Int): List<String> {
    return listOf(
        reportId,
        lineNumber.toString(),
        isFirstLine.toString(),
        isLastModified.toString(),
        editable.toString(),
        label.toString()
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
    val latestModification = methodModificationTimes.maxOrNull() ?: -1
    val firstEditableLine = lines.firstOrNull { it.annotation != null }

    return lines.mapIndexed { index: Int, line: FullStacktraceLineInformation ->
        Features(
            index,
            (line == firstEditableLine).toInt(),
            (methodModificationTimes[index] == latestModification).toInt(),
            (line.annotation != null).toInt()
        )
    }
}

fun lineFromReportDirectory(directory: File): Pair<List<Features>, List<Int>>? {
    val files = directory.listFiles() ?: return null
    val reportFile = files.singleOrNull { file -> file.extension == "json" } ?: return null
    val filesDirectory = files.singleOrNull { file -> file.isDirectory } ?: return null

    val report = Json.decodeFromString<Report>(reportFile.readText())
    val lines = report.lines.map {
        if (it.filename.isEmpty()) return@map FullStacktraceLineInformation(
            it.line, it.filename, it.methodName, null, ""
        )
        val lineFile = File(filesDirectory, it.filename)
        if (!lineFile.exists()) {
            return@map FullStacktraceLineInformation(
                it.line, it.filename, it.methodName, null, ""
            )
        }

        val fileText = lineFile.readText()

        val savedAnnotation = File(filesDirectory, "${it.filename}.json")
        val annotatedFile = Json.decodeFromString<AnnotatedFile?>(savedAnnotation.readText())

        FullStacktraceLineInformation(it.line, it.filename, it.methodName, annotatedFile, fileText)
    }
    val labels = report.lines.map { it.label }

    return Pair(features(lines), labels)
}

fun main(args: Array<String>) {
    val inputPath = args[0]
    val outputFile = args[1]

    if (!File(outputFile).exists()) File(outputFile).createNewFile()

    val directory = File(inputPath)

    if (!directory.exists() || !directory.isDirectory)
        throw InvalidPathException(inputPath, "Empty or broken files directory")

    val features = directory.listFiles()?.flatMap { file ->
        val (features, labels) = lineFromReportDirectory(file) ?: return@flatMap emptyList()

        features.zip(labels).map { (features: Features, label: Int) -> features.toList(file.name, label) }
    } ?: emptyList()


    val columns = Features::class.java.declaredFields.map { it.name }

    csvWriter().open(outputFile) {
        writeRow(listOf("reportId").plus(columns).plus("label"))
        writeRows(features)
    }
}