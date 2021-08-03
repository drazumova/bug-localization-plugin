package com.github.drazumova.plugintest.dataset

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.MessageRevFilter
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import java.io.File

class VCSAnalyzer(path: String, private val issueToReportFile: String) {
    private val gitDir = File(path, ".git")
    private val repo = FileRepository(gitDir)

    companion object {
        const val maxLogDepth = 1000
    }


    private fun commitMessage(reportId: String): String {
        var message = "(?<!ID)EA-"

        csvReader().open(File(issueToReportFile, "issue_report_ids.csv")) {
            val issueNumber = readAllWithHeaderAsSequence().filter { row: Map<String, String> ->
                row["report_id"] == reportId
            }.map { it["issue_id"] }.firstOrNull()
            message += issueNumber
        }
        return message
    }

    private fun commitByMessage(message: String): List<RevCommit> {
        return Git(repo).log().setMaxCount(maxLogDepth)
            .setRevFilter(MessageRevFilter.create(".*$message.*")).call().toList()
    }

    fun commitByHash(hash: String): RevCommit? {
        if (hash.isEmpty()) return null
        return try {
            val commit = RevWalk(repo).parseCommit(ObjectId.fromString(hash))
            commit
        } catch (e: MissingObjectException) {
            null
        }
    }

    fun isMethodFixed(commit: RevCommit, prevCommit: RevCommit, path: String, methodName: String): Boolean {
        if (path.isEmpty() || methodName.isEmpty()) return false

        val textBefore = textByCommit(prevCommit, path)
        if (textBefore.isEmpty()) return false

        val methodRange = methodTextRange(textBefore, methodName)
        if (methodRange.first < 0) return false


        val diffFormatter = DiffFormatter(System.out)
        diffFormatter.pathFilter = PathFilter.create(path)
        diffFormatter.setRepository(repo)
        for (diff in diffFormatter.scan(prevCommit.tree, commit.tree)) {
            val fileHeader = diffFormatter.toFileHeader(diff)
            for (edit in fileHeader.toEditList()) {
                val editedRange = Pair(edit.beginA, edit.endA)
                if (editedRange.cross(methodRange)) {
                    return true
                }
            }
        }
        return false
    }

    fun checkCommits(reportId: String): RevCommit? {
        val message = commitMessage(reportId)
        val commits = commitByMessage(message)
        return commits.firstOrNull()
    }

    fun textByCommit(commit: RevCommit, filepath: String): String {
        if (filepath.isEmpty()) return ""
        try {
            val walk = TreeWalk(repo)
            walk.isRecursive = true
            walk.filter = PathFilter.create(filepath)
            walk.reset(commit.tree)

            if (walk.next()) {
                val stream = ByteArrayOutputStream()
                repo.open(walk.getObjectId(0)).copyTo(stream)
                return stream.toString()
            }
        } catch (e: Exception) {
            println("$e while reading $filepath from ${commit.name}")
        }
        return ""
    }

    fun annotationByCommit(commit: RevCommit, filepath: String): BlameResult? {
        if (filepath.isEmpty()) return null
        return Git(repo).blame().setFilePath(filepath)
            .setStartCommit(commit).setTextComparator(RawTextComparator.WS_IGNORE_ALL).call()
    }
}
