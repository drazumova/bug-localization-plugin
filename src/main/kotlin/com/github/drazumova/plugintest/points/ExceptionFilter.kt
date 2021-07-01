package com.github.drazumova.plugintest.points

import com.github.drazumova.plugintest.utils.VCSAnnotationProvider
import com.intellij.execution.filters.*
import com.intellij.execution.filters.ExceptionWorker.parseExceptionLine
import com.intellij.execution.filters.Filter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.awt.Color


class ExceptionFilter : ExceptionFilterFactory {
    override fun create(searchScope: GlobalSearchScope): Filter {
        return Filter(searchScope)
    }
}

class Filter(scope: GlobalSearchScope) : Filter {
    private val exceptionInfoCache = ExceptionInfoCache(scope)
    private val exceptionWorker = ExceptionWorker(exceptionInfoCache)
    private var collectedInfo: Info? = null
    private val project = scope.project!!
    private val vcsProvider = VCSAnnotationProvider.INSTANCE

    @Suppress("UnstableApiUsage")
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (line.isEmpty()) {
            return processResult()
        }

        val message = ExceptionInfo.parseMessage(line, entireLength)
        message?.let {
            collectedInfo = Info(it, mutableListOf())
        }
        val exceptionLine = parseExceptionLine(line) ?: return null
        val method = exceptionLine.methodNameRange.substring(line)
        val classname = exceptionLine.classFqnRange.substring(line)

        val filter = exceptionWorker.execute(line, entireLength)
        val info = filter?.firstHyperlinkInfo as? FileHyperlinkInfo
        val descriptor = info?.descriptor?.offset ?: return null
        val psiElement = exceptionWorker.file.findElementAt(descriptor) ?: return null
        val virtualFile = psiElement.containingFile.virtualFile

        val resultingLine = Line(
            virtualFile, exceptionLine.lineNumber - 1,
            method, classname, line, entireLength - line.length
        )
        if (collectedInfo != null) {
            collectedInfo?.lines?.add(resultingLine)
        }

        return null
    }

    private fun processResult(): Filter.Result? {
        collectedInfo ?: return null

        val lastModificationTime = collectedInfo!!.lines.map {
            vcsProvider.lastModifiedTime(it.file, it.lineNumber, project)
        }
        val index = lastModificationTime.indexOf(lastModificationTime.maxOrNull())
        val targetLine = collectedInfo!!.lines[index]

        val highlightAttributes = TextAttributes()
        highlightAttributes.backgroundColor = Color.RED
        highlightAttributes.foregroundColor = Color.BLUE
        return Filter.Result(
            targetLine.startOffset,
            targetLine.startOffset + targetLine.lineText.length,
            null,
            highlightAttributes
        )
    }
}

internal data class Line(
    val file: VirtualFile, val lineNumber: Int,
    val methodName: String, val className: String, val lineText: String, val startOffset: Int
)

@Suppress("UnstableApiUsage")
internal data class Info(val exceptionInfo: ExceptionInfo, val lines: MutableList<Line>)