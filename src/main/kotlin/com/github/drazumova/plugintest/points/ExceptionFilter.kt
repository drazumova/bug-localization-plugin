package com.github.drazumova.plugintest.points

import com.github.drazumova.plugintest.utils.PredictionModelConnection
import com.intellij.execution.filters.*
import com.intellij.execution.filters.ExceptionWorker.parseExceptionLine
import com.intellij.execution.filters.Filter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import kotlinx.coroutines.runBlocking
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

        val resultingLine = ExceptionLine(
            virtualFile, psiElement.containingFile, exceptionLine.lineNumber - 1,
            method, classname, line, entireLength - line.length
        )
        if (collectedInfo != null) {
            collectedInfo?.exceptionLines?.add(resultingLine)
        }

        return null
    }

    private fun processResult(): Filter.Result? {
        if (collectedInfo == null) return null
        val probabilitiesList = runBlocking {
            PredictionModelConnection().getProbabilities(collectedInfo!!)
        }
        val index = probabilitiesList.indexOf(probabilitiesList.maxOrNull())

        val targetLine = collectedInfo?.exceptionLines?.get(index) ?: return null

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

data class ExceptionLine(
    val file: VirtualFile,
    val psiFile: PsiFile,
    val lineNumber: Int,
    val methodName: String,
    val className: String,
    val lineText: String,
    val startOffset: Int
)

@Suppress("UnstableApiUsage")
data class Info(val exceptionInfo: ExceptionInfo, val exceptionLines: MutableList<ExceptionLine>)
