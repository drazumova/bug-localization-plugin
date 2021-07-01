package com.github.drazumova.plugintest.utils

import com.intellij.diff.comparison.iterables.DiffIterableUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.diff.Diff
import com.intellij.vcsUtil.VcsUtil
import java.util.*

class VCSAnnotationProviderImpl : VCSAnnotationProvider {
    override fun annotate(file: VirtualFile, project: Project): FileAnnotation? {
        val annotationProvider = VcsUtil.getVcsFor(project, file)?.annotationProvider ?: return null
        return annotationProvider.annotate(file)
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

    override fun lastModifiedTime(file: VirtualFile, line: Int, project: Project): Long {
        val annotationProvider = VcsUtil.getVcsFor(project, file)?.annotationProvider ?: return -1
        val annotation = annotationProvider.annotate(file)
        if (lineChangedLocally(file, line, project)) return Date().time
        return annotation.getLineDate(line)?.time ?: -1
    }
}