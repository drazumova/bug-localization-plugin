package com.github.drazumova.plugintest.points

import com.intellij.execution.filters.*
import com.intellij.execution.filters.ExceptionWorker.parseExceptionLine
import com.intellij.execution.filters.Filter
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Consumer


class ExceptionFilter : ExceptionFilterFactory {
    companion object {
        var counter : Int = 0
    }

    override fun create(searchScope: GlobalSearchScope): Filter {
        counter += 1
        return Filter("filter $counter", searchScope)
    }
}

class Filter(private val name: String, private val scope: GlobalSearchScope) : Filter, FilterMixin {
    private val exceptionInfoCache = ExceptionInfoCache(scope)
    private val exceptionWorker = ExceptionWorker(exceptionInfoCache)
    private var collectedInfo : Info? = null

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
            processResult()
            return null
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

        val resultingLine = Line(virtualFile, psiElement.containingFile, exceptionLine.lineNumber, method, classname)
        if (collectedInfo != null) {
            collectedInfo?.lines?.add(resultingLine)
        } else {
            println("Lost line $line")
        }
        return null
    }

    private fun processResult() {
        collectedInfo ?: return
        println("process result")
        println("${collectedInfo?.exceptionInfo?.exceptionClassName} with ${collectedInfo?.lines?.size} lines")
    }
}

internal data class Line(val file : VirtualFile, val psiFile : PsiElement, val lineNumber: Int,
                val methodName: String, val className: String)

internal data class Info(val exceptionInfo: ExceptionInfo, val lines: MutableList<Line>)