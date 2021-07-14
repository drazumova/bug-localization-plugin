package com.github.drazumova.plugintest.dataset

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.eclipse.jgit.revwalk.RevCommit


@Serializable
data class FullStacktrace(
    val reportId: String,
    val exceptionClass: String,
    val initCommit: Boolean,
    val lines: List<LineInformation>
)

@Serializable
data class LineInformation(
    val lineNumber: Int, val filename: String,
    val lastModified: Int, val lastModifiedFile: Int, val lastModifiedMethod: Int,
    val labelMethod: Boolean
)

private fun JsonObject.getOrNull(field: String): JsonElement? {
    val value = get(field)
    if (value is JsonNull || value == null) return null
    return value
}

internal fun lineInformation(
    analyzer: Analyzer, frame: JsonObject,
    commit: RevCommit, prevCommit: RevCommit,
    initCommit: RevCommit
): LineInformation {
    val filename = frame.getOrNull("file_name")?.jsonPrimitive?.content ?: ""
    val line = frame.getOrNull("line_number")?.jsonPrimitive?.int ?: -1
    val path = frame.getOrNull("path")?.jsonPrimitive?.content ?: ""
    val method = frame.getOrNull("method_name")?.jsonPrimitive?.content ?: ""

    if (path.isEmpty() || line < 0 || method.isEmpty())
        return LineInformation(line, filename, -1, -1, -1, false)

    val isModified = analyzer.isMethodFixed(commit, prevCommit, path, method)

    val fileText = analyzer.textByCommit(initCommit, path)
    val methodRange = getMethodTextRange(fileText, method)
    val methodModificationTime = analyzer.getModificationTime(initCommit, methodRange.first, methodRange.second, path)
    val lineModificationTime = analyzer.getModificationTime(initCommit, line, line, path)

    val fileModificationTime = analyzer.isFileModified(commit, path)

//    println("For $filename $methodRange and line $line")
//    println("And times $methodModificationTime $lineModificationTime $fileModificationTime")
    return LineInformation(
        line, filename,
        lastModified = lineModificationTime,
        lastModifiedFile = fileModificationTime,
        lastModifiedMethod = methodModificationTime,
        labelMethod = isModified
    )
}

internal fun getFixCommit(hash: String, reportId: String, analyzer: Analyzer): RevCommit? {
    return analyzer.commitByHash(hash) ?: analyzer.checkCommits(reportId)
}

internal fun JsonObject.toReport(reportId: String, analyzer: Analyzer): FullStacktrace? {
    val exceptionClass = (getOrNull("class") ?: return null).jsonArray.toList().map { it.jsonPrimitive.content }


    val hash = getOrNull("hash")?.jsonPrimitive?.content ?: ""
    getOrDefault("hash", null)
    val fixCommit = getFixCommit(hash, reportId, analyzer) ?: return null
    println("Commit message: ${fixCommit.shortMessage}")

    val prevCommit = analyzer.commitByHash(fixCommit.parents.first().name) ?: return null

    val hash1 = getOrNull("commit")?.jsonObject?.getOrNull("hash")?.jsonPrimitive?.content ?: ""
    val initCommit = analyzer.commitByHash(hash1) ?: prevCommit
    if (initCommit == prevCommit) println("No init commit,  last used")

    if ((getOrNull("frames") ?: return null).jsonArray.size > 100) {
        println("Too long stacktrace")
        return null
    }

    val frames = (getOrNull("frames") ?: return null).jsonArray.map {
        lineInformation(analyzer, it.jsonObject, fixCommit, prevCommit, initCommit)
    }
    return FullStacktrace(
        reportId,
        exceptionClass.firstOrNull() ?: "",
        initCommit != prevCommit,
        frames
    )

}

