package com.github.drazumova.plugintest.dataset

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.InvalidPathException
import java.util.*


data class Features(
    val reportId: String,
    val lineNumber: Int,
    val inFileNumber: Int,
    val label: Int,
    val fileModificationTime: Int,
    val methodModificationTime: Int,
    val lineModificationTime: Int,
    val commiter: Boolean,
    val lineModificationTimeFrame: Int,
)

internal fun Features.toList(): List<String> {
    return listOf(
        reportId,
        lineNumber.toString(),
        inFileNumber.toString(),
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
    return report.lines.filter { it.line > 0 }
        .mapIndexed { index: Int, line: ExceptionLine ->
            if (!File(filesDirectory, line.filename).exists()) {
                return@mapIndexed Features(
                    report.reportId, index, line.line, line.label,
                    -1, -1, -1, true, -1
                )
            }
            val fileText = File(filesDirectory, line.filename).readText()
            val methodRange = methodTextRange(fileText, line.methodName)
            val annotationFile = File(filesDirectory, "${line.filename}.json")
            val file = Json.decodeFromString<AnnotatedFile?>(annotationFile.readText())
                ?: return@mapIndexed Features(
                    report.reportId, index, line.line, line.label,
                    -1, -1, -1, true, -1
                )
            val fileModificationTime = file.lineAnnotation
                .map { it.time }.maxOrNull() ?: -1
            val methodModificationTime =
                if (methodRange.first != -1)
                    file.lineAnnotation
                        .slice(IntRange(methodRange.first, methodRange.second)).map { it.time }.maxOrNull() ?: -1
                else
                    -1

            val lineTimeModification = file.lineAnnotation.getOrNull(line.line)?.time ?: -1

            Features(
                report.reportId,
                index,
                line.line,
                line.label,
                fileModificationTime,
                methodModificationTime,
                lineTimeModification,
                file.lineAnnotation.getOrNull(line.line)?.author != file.lineAnnotation.getOrNull(line.line)?.commiter,
                Date(lineTimeModification.toLong()).hours
            )
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