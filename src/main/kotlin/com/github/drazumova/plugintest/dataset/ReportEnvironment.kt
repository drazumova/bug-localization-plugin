package com.github.drazumova.plugintest.dataset

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.eclipse.jgit.revwalk.RevCommit
import java.io.File

@Serializable
data class Report(
    val reportId: String,
    val fixCommit: Commit, val initCommit: Commit,
    val exceptionClass: String, val lines: List<ParsedExceptionLine>
)

@Serializable
data class ParsedExceptionLine(
    val filename: String, val methodName: String,
    val line: Int, val path: String,
    val label: Int
)

@Serializable
data class AnnotatedFile(val filename: String, val path: String, val lineAnnotation: List<Commit?>)

@Serializable
data class Commit   (
    val hash: String, val message: String,
    val author: Person, val commiter: Person, val time: Long
)

@Serializable
data class Person(val name: String, val email: String)


private fun RevCommit.toCommit(): Commit {
    return Commit(
        name, shortMessage, Person(authorIdent.name, authorIdent.emailAddress),
        Person(committerIdent.name, committerIdent.emailAddress), commitTime.toLong()
    )
}

private fun JsonObject.getOrNull(field: String): JsonElement? {
    val value = get(field)
    if (value is JsonNull || value == null) return null
    return value
}

class ReportEnvironment private constructor(
    private val analyzer: Analyzer, id: String, private val report: JsonObject,
    private val initCommit: RevCommit, private val fixCommit: RevCommit,
    savePath: String
) {

    private val directory = File(savePath, id)
    private val filesDirectory = File(directory, "files")

    init {
        directory.mkdirs()
        filesDirectory.mkdirs()
    }

    companion object {
        private fun fixCommit(analyzer: Analyzer, hash: String, reportId: String): RevCommit? {
            return analyzer.commitByHash(hash) ?: analyzer.checkCommits(reportId)
        }

        private fun previousCommit(analyzer: Analyzer, commit: RevCommit): RevCommit? {
            val parent = commit.parents.firstOrNull() ?: return null
            return analyzer.commitByHash(parent.name)
        }

        fun parseReport(analyzer: Analyzer, report: JsonObject, path: String): ReportEnvironment? {
            val initHash = report.getOrNull("commit")?.jsonObject?.getOrNull("hash")?.jsonPrimitive?.content ?: ""
            val fixHash = report.getOrNull("hash")?.jsonPrimitive?.content ?: ""
            val id = report["id"]?.jsonPrimitive?.content ?: return null

            val fixCommit = fixCommit(analyzer, fixHash, id) ?: return null
            val prevCommit = previousCommit(analyzer, fixCommit)
            val initCommit = analyzer.commitByHash(initHash) ?: prevCommit ?: return null

            return ReportEnvironment(analyzer, id, report, initCommit, fixCommit, path)
        }
    }

    fun save() {
        val id = report["id"]!!.jsonPrimitive.content
        val exceptionClass = report["class"]?.jsonArray?.toList()?.map { it.jsonPrimitive.content } ?: emptyList()

        if (report["frames"]?.jsonArray?.size ?: 0 > 200) {
            println("Too long stacktrace")
            return
        }

        val lines = report["frames"]?.jsonArray?.map {
            val frame = it.jsonObject
            val filename = frame.getOrNull("file_name")?.jsonPrimitive?.content ?: ""
            val line = frame.getOrNull("line_number")?.jsonPrimitive?.int ?: -1
            val path = frame.getOrNull("path")?.jsonPrimitive?.content ?: ""
            val method = frame.getOrNull("method_name")?.jsonPrimitive?.content ?: ""
            val label = frame.getOrNull("label")?.jsonPrimitive?.int ?: 0

            if (filename.isNotEmpty() && path.isNotEmpty() && line >= 0) {
                val fileText = analyzer.textByCommit(initCommit, path)
                saveToFile(File(filesDirectory, filename), fileText)

                val annotatedFile = annotatedFile(filename, path)
                saveToFile(File(filesDirectory, "$filename.json"), Json.encodeToString(annotatedFile))
            }

            ParsedExceptionLine(filename, method, line, path, label)
        } ?: emptyList()

        val report = Report(id, fixCommit.toCommit(), initCommit.toCommit(), Json.encodeToString(exceptionClass), lines)
        saveToFile(File(directory, "$id.json"), Json.encodeToString(report))
    }


    private fun saveToFile(file: File, text: String) {
        if (!file.exists()) file.createNewFile()
        file.writeText(text)
    }

    private fun annotatedFile(filename: String, path: String): AnnotatedFile? {
        val blame = analyzer.annotationByCommit(initCommit, path) ?: return null

        var len = 0
        try {
            while (blame.hasSourceData(len)) len += 1
        } catch (_: ArrayIndexOutOfBoundsException) {
            // ... (
        }

        val lines = (0 until len).map {
            blame.getSourceCommit(it).toCommit()
        }

        return AnnotatedFile(filename, path, lines)
    }
}