package com.github.drazumova.plugintest.points

import com.github.drazumova.plugintest.utils.VCSAnnotationProvider
import com.intellij.execution.filters.*
import com.intellij.execution.filters.ExceptionWorker.parseExceptionLine
import com.intellij.execution.filters.Filter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Consumer
import java.awt.Color


class ExceptionFilter : ExceptionFilterFactory {
    companion object {
        var counter: Int = 0
    }

    override fun create(searchScope: GlobalSearchScope): Filter {
        counter += 1
        return Filter("filter $counter", searchScope)
    }
}

class Filter(private val name: String, scope: GlobalSearchScope) : Filter, FilterMixin {
    private val exceptionInfoCache = ExceptionInfoCache(scope)
    private val exceptionWorker = ExceptionWorker(exceptionInfoCache)
    private var collectedInfo: Info? = null
    private val project = scope.project!!
    private val vcsProvider = VCSAnnotationProvider.INSTANCE

    override fun shouldRunHeavy(): Boolean {
        return true
    }

    override fun applyHeavyFilter(
        copiedFragment: Document,
        startOffset: Int,
        startLineNumber: Int,
        consumer: Consumer<in FilterMixin.AdditionalHighlight>
    ) {
        println(copiedFragment.text)
    }

    override fun getUpdateMessage(): String {
        return ""
    }

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (line.isEmpty()) {
            return processResult()
        }

        val message = ExceptionInfo.parseMessage(line, entireLength)
        message?.let {
            collectedInfo = Info(it, mutableListOf())
        }
        val filter = exceptionWorker.execute(line, entireLength) ?: return null
        val exceptionLine = parseExceptionLine(line) ?: return null
        val method = exceptionLine.methodNameRange.substring(line)
        val classname = exceptionLine.classFqnRange.substring(line)

        val info = filter.firstHyperlinkInfo as? FileHyperlinkInfo ?: return null
        val descriptor = info.descriptor?.offset ?: return null
        val psiElement = exceptionWorker.file.findElementAt(descriptor) ?: return null
        val virtualFile = psiElement.containingFile.virtualFile

        val resultingLine = Line(
            virtualFile, exceptionLine.lineNumber - 1,
            method, classname, line, entireLength - line.length
        )
        if (collectedInfo != null) {
            collectedInfo?.lines?.add(resultingLine)
        } else {
            println("Lost line $line")
        }

        return null
    }

    private fun processResult(): Filter.Result? {
        collectedInfo ?: return null

        val lastModificationTime = collectedInfo!!.lines.map {
            vcsProvider.lastModifiedTime(it.file, it.lineNumber, project)
        }
        val index = lastModificationTime.indexOf(lastModificationTime.maxOrNull() ?: lastModificationTime.first())
        val targetLine = collectedInfo!!.lines[index]

//        val virtualFile = targetLine.file
//        val linkInfo = HyperlinkInfoFactory.getInstance().createMultipleFilesHyperlinkInfo(
//            listOf(virtualFile), targetLine.lineNumber - 1, scope.project!!, null)

        val ta = TextAttributes()
        ta.backgroundColor = Color.RED
        ta.foregroundColor = Color.BLUE
        return Filter.Result(
            targetLine.startOffset,
            targetLine.startOffset + targetLine.lineText.length,
            null,
            ta
        )
    }
}

internal data class Line(
    val file: VirtualFile, val lineNumber: Int,
    val methodName: String, val className: String, val lineText: String, val startOffset: Int
)

internal data class Info(val exceptionInfo: ExceptionInfo, val lines: MutableList<Line>)