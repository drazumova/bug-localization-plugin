package com.github.drazumova.plugintest.dataset

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.InvalidPathException


data class Features(
    val reportId: String, val label: Int, val fileModificationTime: Int, val methodModificationTime: Int,
    val lineModificationTime: Int
)

internal fun Features.toList(): List<String> {
    return listOf(
        reportId,
        label.toString(),
        fileModificationTime.toString(),
        methodModificationTime.toString(),
        lineModificationTime.toString()
    )
}

fun lineFromReportDirectory(directory: File): List<Features>? {
    val files = directory.listFiles() ?: return null
    val reportFile = files.singleOrNull { file -> file.extension == "json" } ?: return null
    val filesDirectory = files.singleOrNull { file -> file.isDirectory } ?: return null

    val report = Json.decodeFromString<Report>(reportFile.readText())
    return report.lines.filter { it.filename.isNotEmpty() && it.line > 0 }.map { line ->
        val annotationFile = File(filesDirectory, "${line.filename}.json")
        val fileText = File(filesDirectory, line.filename).readText()
        val methodRange = methodTextRange(fileText, line.methodName)


        val file = Json.decodeFromString<AnnotatedFile?>(annotationFile.readText())

        val fileModificationTime = file?.lineAnnotation
            ?.map { it.time }?.maxOrNull() ?: -1
        val methodModificationTime =
            if (methodRange.first != -1)
                file?.lineAnnotation
                    ?.slice(IntRange(methodRange.first, methodRange.second))?.map { it.time }?.maxOrNull() ?: -1
            else
                -1

        val lineTimeModification =
            if (file != null && line.line < file.lineAnnotation.size) file.lineAnnotation[line.line].time else -1

        Features(report.reportId, line.label, fileModificationTime, methodModificationTime, lineTimeModification)
    }
}

fun main(args: Array<String>) {
    val inputPath = args[0]
    val outputFile = args[1]

    if (!File(outputFile).exists()) File(outputFile).createNewFile()

    val directory = File(inputPath)

    if (!directory.exists() || !directory.isDirectory)
        throw InvalidPathException(inputPath, "Empty or broken files directory")

    val features = directory.listFiles()?.flatMap {
        lineFromReportDirectory(it) ?: emptyList()
    }?.map { it.toList() } ?: emptyList()

    val columns = Features::class.java.declaredFields.map { it.name }

    csvWriter().open("test.csv") {
        writeRow(columns)
        writeRows(features)
    }

}