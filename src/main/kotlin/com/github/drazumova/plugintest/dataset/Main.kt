package com.github.drazumova.plugintest.dataset

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

    val outputPath = File(output)
    if (!outputPath.exists()) outputPath.mkdirs()

    val allFiles = directory.listFiles()?.filter { it.isFile && it.extension == "json" }
        ?: throw InvalidPathException(reportsDir, "Empty or broken files directory")

    allFiles.filterNot { excludedList.contains(it.name) }.take(10).forEach { file ->
        val reportId = file.nameWithoutExtension
        println(reportId)
        val json = Json.parseToJsonElement(file.readText()).jsonObject

        val dataset = Dataset.createDataset(analyzer, json, outputPath.path)
        if (dataset == null) println("Failed to parse $reportId")
        dataset?.saveReport()
    }
}
