package com.github.drazumova.plugintest.vcs

import com.github.drazumova.plugintest.dataset.AnnotatedFile
import com.github.drazumova.plugintest.dataset.Commit
import com.github.drazumova.plugintest.dataset.Person
import com.intellij.diff.comparison.iterables.DiffIterableUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.diff.Diff
import com.intellij.vcs.log.VcsUser
import com.intellij.vcsUtil.VcsUtil
import git4idea.history.GitHistoryUtils
import java.lang.Integer.max
import java.util.*

class VCSAnnotationProviderImpl : VCSAnnotationProvider {
    override fun annotate(file: VirtualFile, project: Project): AnnotatedFile? {
        val annotationProvider = VcsUtil.getVcsFor(project, file)?.annotationProvider ?: return null
        val change = ChangeListManager.getInstance(project).getChange(file)
        val changedLines = mutableSetOf<Int>()
        val lines = change?.afterRevision?.content?.split("\n")?.size ?: 0
        if (change?.afterRevision != null) {
            if (change.beforeRevision == null) {
                changedLines.addAll((0 until lines).toList())
            } else {
                // todo cache for diffs
                val after = change.afterRevision?.content ?: ""
                val before = change.beforeRevision?.content ?: ""
                val diff = Diff.buildChanges(before, after)
                val changes = DiffIterableUtil.create(diff, before.length, after.length).changes()
                changes.forEach {
                    changedLines.addAll((it.start2..it.end2))
                }
            }
        }
        val fileAnnotation = annotationProvider.annotate(file)
        val vcsRoot = VcsUtil.getVcsRootFor(project, file)!!

        val lineCount = max(lines, fileAnnotation.lineCount)

        val annotatedLines = (0 until lineCount).map {
            if (it in changedLines) return@map null
            val lineRevision = fileAnnotation.getLineRevisionNumber(it) ?: return@map null
            val commitHash = lineRevision.asString()
            val metadata =
                GitHistoryUtils.collectCommitsMetadata(project, vcsRoot, commitHash)?.firstOrNull() ?: return@map null
            Commit(
                commitHash, metadata.fullMessage, metadata.author.toPerson(),
                metadata.committer.toPerson(), metadata.timestamp
            )
        }
        return AnnotatedFile(filename = file.name, path = file.canonicalPath ?: "", annotatedLines)
    }

    private fun VcsUser.toPerson(): Person {
        return Person(name, email)
    }

    private fun lineChangedLocally(file: VirtualFile, line: Int, project: Project): Boolean {
        val change = ChangeListManager.getInstance(project).getChange(file)
        val after = change?.afterRevision?.content ?: return false
        val before = change.beforeRevision?.content ?: return true
        val diff = Diff.buildChanges(before, after)
        val changes = DiffIterableUtil.create(diff, before.length, after.length).changes()
        return Sequence { changes }.any {
            it.end2 > line && it.start2 <= line
        }
    }

    override fun lastModifiedTime(file: VirtualFile, line: Int, project: Project): Long {
        val annotation = annotate(file, project)
        if (lineChangedLocally(file, line, project)) return Date().time
        return annotation?.lineAnnotation?.get(line)?.time ?: -1
    }
}
