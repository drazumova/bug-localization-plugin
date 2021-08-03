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


    val analyzer = VCSAnalyzer(intellijRepo, reportsIssuesDir)
    val directory = File(reportsDir)
    val excludedList = listOf("1169266", "1744445")

    val outputPath = File(output)
    if (!outputPath.exists()) outputPath.mkdirs()
    val collected = outputPath.listFiles()?.toList() ?: emptyList<File>()

    val allFiles = directory.listFiles()?.filter { it.isFile && it.extension == "json" }
        ?: throw InvalidPathException(reportsDir, "Empty or broken files directory")

    val all = allFiles.filterNot { excludedList.contains(it.name) }.size
    var cnt = 0

    allFiles.filterNot { excludedList.contains(it.name) }.forEach { file ->
        val reportId = file.nameWithoutExtension
        println(reportId)
        if (collected.any {it.name == reportId}) {
            cnt += 1
            println("Already got $reportId")
            return@forEach
        }
        val json = Json.parseToJsonElement(file.readText()).jsonObject

        val parsedInfo = ReportEnvironment.parseReport(analyzer, json, outputPath.path)
        cnt += 1
        println("$cnt / $all")
        if (parsedInfo == null) println("Failed to parse $reportId")
        parsedInfo?.save()
    }
}
