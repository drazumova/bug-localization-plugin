package com.github.drazumova.plugintest.utils

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vfs.VirtualFile

interface VCSAnnotationProvider {
    companion object {
        private val EP_NAME: ExtensionPointName<VCSAnnotationProvider> =
            ExtensionPointName.create(
                "com.github.drazumova.plugintest.vcsInformationProvider"
            )

        val INSTANCE = CompositeAnnotationProvider(EP_NAME.extensions.plus(VCSAnnotationProviderImpl()).toList())
    }

    /**
     * java.util.Date time in milliseconds since last modification.
     * Or -1 if there are no known changes.
     */
    fun lastModifiedTime(file: VirtualFile, line: Int, project: Project): Long
    fun annotate(file: VirtualFile, project: Project): FileAnnotation?
}

class CompositeAnnotationProvider(private val providers: List<VCSAnnotationProvider>) : VCSAnnotationProvider {
    override fun lastModifiedTime(file: VirtualFile, line: Int, project: Project): Long {
        return providers.map { it.lastModifiedTime(file, line, project) }.maxOrNull() ?: -1
    }

    override fun annotate(file: VirtualFile, project: Project): FileAnnotation? {
        return providers.mapNotNull { it.annotate(file, project) }.firstOrNull()
    }
}
