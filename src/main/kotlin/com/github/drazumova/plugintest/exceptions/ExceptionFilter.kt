package com.github.drazumova.plugintest.exceptions

import com.intellij.execution.filters.*
import com.intellij.execution.filters.ExceptionWorker.parseExceptionLine
import com.intellij.execution.filters.Filter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import java.awt.Color

class ExceptionFilter : ExceptionFilterFactory {
    override fun create(searchScope: GlobalSearchScope): Filter {
        return Filter(searchScope)
    }
}

class Highlighter(private val k: Int = 1) {
    companion object {
        val redHighlight = TextAttributes().also {
            it.backgroundColor = Color.RED
            it.foregroundColor = Color.DARK_GRAY
        }

        val orangeHighlight = TextAttributes().also {
            it.backgroundColor = Color.ORANGE
            it.foregroundColor = Color.DARK_GRAY
        }

        val yellowHighlight = TextAttributes().also {
            it.backgroundColor = Color.YELLOW
            it.foregroundColor = Color.DARK_GRAY
        }

        val greyHighlight = TextAttributes().also {
            it.backgroundColor = Color.GRAY
        }
    }

    private fun highlightLine(targetLine: ExceptionLine, ta: TextAttributes): Filter.Result {
        return Filter.Result(
            targetLine.startOffset,
            targetLine.startOffset + targetLine.lineText.length,
            null,
            ta
        )
    }

    fun highlight(probabilities: List<Double>, exceptionLines: List<ExceptionLine>): Filter.Result? {
        val top = probabilities.toSet().sorted().takeLast(k).reversed().filter { it != 0.0 }
        val highlights = exceptionLines.mapIndexed { index: Int, exceptionLine: ExceptionLine ->
            if (probabilities[index] in top) {
                val textAttributes = when(top.indexOf(probabilities[index])) {
                    0 -> redHighlight
                    1 -> orangeHighlight
                    2 -> yellowHighlight
                    else -> greyHighlight
                }
                highlightLine(exceptionLine, textAttributes)
            }
            else null
        }.filterNotNull()
        return Filter.Result(highlights)
    }
}

class Filter(private val scope: GlobalSearchScope) : Filter {
    private val exceptionInfoCache = ExceptionInfoCache(scope)
    private val exceptionWorker = ExceptionWorker(exceptionInfoCache)
    private var infoBuilder = ExceptionInfoBuilder()
    private val highlighter = Highlighter(3)

    @Suppress("UnstableApiUsage")
    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (line.isEmpty()) {
            return processResult()
        }

        val message = ExceptionInfo.parseMessage(line, entireLength)
        infoBuilder.addMessage(message, line)

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
        infoBuilder.addLine(resultingLine)

        return null
    }

    private fun processResult(): Filter.Result? {
        val collectedInfo = infoBuilder.build() ?: return null

        val service = exceptionInfoCache.project.getService(ProbabilitiesCacheService::class.java)
        val probabilitiesList = service.checkResultForStacktrace(collectedInfo) ?: return null

        return highlighter.highlight(probabilitiesList, collectedInfo.exceptionLines)
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
data class Info(val exceptionInfo: ExceptionInfo, val exceptionLines: List<ExceptionLine>, val fullText: String)

@Suppress("UnstableApiUsage")
class ExceptionInfoBuilder {
    private val textBuilder = StringBuilder()
    private var exceptionInfo: ExceptionInfo? = null
    private val lines = mutableListOf<ExceptionLine>()


    fun build(): Info? {
        return if (exceptionInfo != null && lines.isNotEmpty()) {
            Info(exceptionInfo!!, lines, textBuilder.toString())
        } else null
    }

    fun addMessage(parsedMessage: ExceptionInfo?, line: String) {
        exceptionInfo = exceptionInfo ?: parsedMessage
        parsedMessage?.let {
            textBuilder.append(line)
        }
    }

    fun addLine(line: ExceptionLine) {
        lines.add(line)
        textBuilder.append(line.lineText)
    }

    fun addLineText(lineText: String) {
        textBuilder.append(lineText)
    }

}
