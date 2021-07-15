package com.github.drazumova.plugintest.dataset

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.nio.file.InvalidPathException

fun main(args: Array<String>) {
    val reportsIssuesDir = args[0]
    val reportsDir = args[1]
    val intellijRepo = args[2]
    val output = args[3]


    val analyzer = Analyzer(intellijRepo, reportsIssuesDir)
    val directory = File(reportsDir)
    val excludedList = listOf("1169266", "1744445")

    val outputFile = File(output)
    if (!outputFile.exists()) outputFile.createNewFile()

    val allFiles = directory.listFiles()?.filter { it.isFile && it.extension == "json" }
        ?: throw InvalidPathException(reportsDir, "Empty or broken files directory")

    val reports = allFiles.filterNot { excludedList.contains(it.name) }.take(100).mapNotNull { file ->
        val reportId = file.nameWithoutExtension
        println(reportId)
        val json = Json.parseToJsonElement(file.readText()).jsonObject

        val report = json.toReport(reportId, analyzer)
        if (report == null) println("Failed to parse $reportId")
        report
    }

    outputFile.writeText(
        Json.encodeToString(
            reports
        )
    )
}
