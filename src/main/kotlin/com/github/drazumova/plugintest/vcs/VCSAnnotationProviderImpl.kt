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
import java.util.*

class VCSAnnotationProviderImpl : VCSAnnotationProvider {
    override fun annotate(file: VirtualFile, project: Project): AnnotatedFile? {
        val annotationProvider = VcsUtil.getVcsFor(project, file)?.annotationProvider ?: return null
        val fileAnnotation = annotationProvider.annotate(file)
        val vcsRoot = VcsUtil.getVcsRootFor(project, file)!!

        val annotatedLines = (0..fileAnnotation.lineCount).mapNotNull {
            val lineRevision = fileAnnotation.getLineRevisionNumber(it) ?: return null
            val commitHash = lineRevision.asString()
            val metadata =
                GitHistoryUtils.collectCommitsMetadata(project, vcsRoot, commitHash)?.firstOrNull() ?: return null

            Commit(
                commitHash, metadata.fullMessage, metadata.author.toPerson(),
                metadata.committer.toPerson(), metadata.timestamp.toInt()
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
        val before = change.beforeRevision?.content ?: return false
        val diff = Diff.buildChanges(before, after)
        val changes = DiffIterableUtil.create(diff, before.length, after.length).changes()
        return Sequence { changes }.any {
            it.end2 > line && it.start2 <= line
        }
    }

    override fun lastModifiedTime(file: VirtualFile, line: Int, project: Project): Int {
        val annotation = annotate(file, project)
        if (lineChangedLocally(file, line, project)) return Date().time.toInt()
        return annotation?.lineAnnotation?.get(line)?.time ?: -1
    }
}
